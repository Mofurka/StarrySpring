package irden.space.proxy.plugin.irden.integration.web.rest.v1.statistics;

import irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults;
import irden.space.proxy.plugin.irden.integration.persistence.model.PlayerAttributesEntity;
import irden.space.proxy.plugin.irden.integration.persistence.repository.PlayerAttributesRepository;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountOwnerType;
import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticRecordEntity;
import irden.space.proxy.plugin.irden.persistence.repository.AccountRepository;
import irden.space.proxy.plugin.irden.persistence.repository.statistic.PlayerStatisticRecordRepository;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsHandler {

    private final PlayerAttributesRepository playerAttributesRepository;
    private final PlayerStatisticRecordRepository playerStatisticRecordRepository;
    private final AccountRepository accountRepository;
    private final PlayerManagerApi playerManagerApi;

    public StatisticsResponse handleAllPlayers() {
        return aggregate(playerAttributesRepository.findAll());
    }

    public StatisticsResponse handlePlayerByApplicationId(long applicationId) {
        return aggregate(playerAttributesRepository.findAllByApplicationId(applicationId));
    }

    private StatisticsResponse aggregate(List<PlayerAttributesEntity> attributes) {
        Map<Long, List<String>> uuidsByApplication = attributes.stream()
                .filter(attribute -> attribute.getApplicationId() != null)
                .collect(Collectors.groupingBy(
                        PlayerAttributesEntity::getApplicationId,
                        TreeMap::new,
                        Collectors.mapping(PlayerAttributesEntity::getPlayerUuid, Collectors.toList())
                ));

        if (uuidsByApplication.isEmpty()) {
            return new StatisticsResponse(Map.of());
        }

        Set<String> uuids = uuidsByApplication.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Map<String, List<PlayerStatisticRecordEntity>> recordsByUuid =
                playerStatisticRecordRepository.findAllByPlayerUuidIn(uuids).stream()
                        .collect(Collectors.groupingBy(PlayerStatisticRecordEntity::getPlayerUuid));

        Set<String> onlineUuids = playerManagerApi.onlinePlayers().stream()
                .map(player -> player.uuid().toString())
                .collect(Collectors.toSet());


        Set<String> accountOwnerIds = uuidsByApplication.keySet().stream()
                .map(PlayerAccountDefaults::playerOwnerId)
                .collect(Collectors.toSet());

        Map<String, Long> balanceByOwnerId = accountRepository.findByOwnerTypeAndOwnerIdInAndAccountCode(
                        AccountOwnerType.CHARACTER,
                        accountOwnerIds,
                        PlayerAccountDefaults.PLAYER_DEFAULT_ACCOUNT_CODE
                )
                .stream()
                .collect(Collectors.toMap(
                        AccountEntity::getOwnerId,
                        AccountEntity::getBalance,
                        Long::sum
                ));

        Map<Long, StatisticsByPlayer> players = new LinkedHashMap<>();
        uuidsByApplication.forEach((applicationId, applicationUuids) -> {
            Map<Integer, Map<Month, PeriodAccumulator>> merged =
                    mergeStatistics(applicationUuids, recordsByUuid);

            if (merged.isEmpty()) {
                return;
            }

            players.put(applicationId, new StatisticsByPlayer(
                    lastSeen(applicationUuids),
                    toStatistics(merged),
                    List.copyOf(applicationUuids),
                    applicationUuids.stream().anyMatch(onlineUuids::contains),
                    balanceByOwnerId.getOrDefault(PlayerAccountDefaults.playerOwnerId(applicationId), 0L)
            ));
        });

        return new StatisticsResponse(players);
    }

    private Map<Integer, Map<Month, PeriodAccumulator>> mergeStatistics(
            List<String> applicationUuids,
            Map<String, List<PlayerStatisticRecordEntity>> recordsByUuid
    ) {
        Map<Integer, Map<Month, PeriodAccumulator>> merged = new TreeMap<>();

        for (String uuid : applicationUuids) {
            for (PlayerStatisticRecordEntity record : recordsByUuid.getOrDefault(uuid, List.of())) {
                merged
                        .computeIfAbsent(record.getYear(), ignored -> new TreeMap<>())
                        .computeIfAbsent(record.getMonth(), ignored -> new PeriodAccumulator())
                        .add(record);
            }
        }

        return merged;
    }

    private Map<Integer, Map<Month, StatisticPeriodDto>> toStatistics(
            Map<Integer, Map<Month, PeriodAccumulator>> merged
    ) {
        Map<Integer, Map<Month, StatisticPeriodDto>> statistics = new LinkedHashMap<>();

        merged.forEach((year, months) -> {
            Map<Month, StatisticPeriodDto> periods = new LinkedHashMap<>();
            months.forEach((month, accumulator) -> periods.put(month, accumulator.toDto()));
            statistics.put(year, periods);
        });

        return statistics;
    }

    private LocalDateTime lastSeen(List<String> applicationUuids) {
        return applicationUuids.stream()
                .map(uuid -> playerManagerApi.findPlayerByUuid(uuid, false))
                .flatMap(Optional::stream)
                .map(Player::lastSeen)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private static final class PeriodAccumulator {

        private long timeInGame;
        private int words;
        private int characters;
        private int messages;

        void add(PlayerStatisticRecordEntity record) {
            timeInGame += record.getInGameTimeSeconds();
            words += record.getWordCount();
            characters += record.getCharactersCount();
            messages += record.getMessageCount();
        }

        StatisticPeriodDto toDto() {
            return new StatisticPeriodDto(timeInGame, words, characters, messages);
        }
    }
}
