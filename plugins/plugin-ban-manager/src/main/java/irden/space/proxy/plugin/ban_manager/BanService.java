package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.ban_manager.model.BanOperationResult;
import irden.space.proxy.plugin.ban_manager.persistence.BanRecordJdbcRepository;
import irden.space.proxy.plugin.ban_manager.persistence.model.BanRecord;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class BanService {

    private static final Logger log = LoggerFactory.getLogger(BanService.class);
    private static final String IP_ADDRESS_PATTERN = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b";

    private final BanRecordJdbcRepository banRecordRepository;
    private final PlayerManagerApi playerManagerApi;

    public Optional<BanRecord> findActiveBan(String name, String playerUuid, String ipAddress) {
        BanRecord probe = BanRecord.builder()
                .name(name)
                .playerUuid(playerUuid)
                .ipAddress(ipAddress)
                .build();
        return banRecordRepository.findActiveBanByBanRecord(probe);
    }

    public boolean unban(String targetPlayer) {
        if (isIpAddress(targetPlayer)) {
            int updated = banRecordRepository.deactivateActiveBanByIpAddress(targetPlayer);
            if (updated > 0) {
                log.info("Removed active IP ban for {}", targetPlayer);
            }
            return updated > 0;
        }

        Optional<Player> optionalPlayer = playerManagerApi.findPlayer(targetPlayer, false);
        if (optionalPlayer.isEmpty()) {
            return false;
        }

        Player player = optionalPlayer.get();
        int updated = banRecordRepository.deactivateActiveBanByPlayer(player.uuid().toString(), player.name());
        if (updated > 0) {
            log.info("Removed active ban for player {} ({})", player.name(), player.uuid());
        }
        return updated > 0;
    }

    public BanOperationResult ban(String targetPlayer, String bannedBy, String durationStr, String reason) {
        boolean permanent = durationStr.equalsIgnoreCase("permanent");
        LocalDateTime expiresAt = null;

        if (!permanent) {
            try {
                expiresAt = parseExpiresAt(durationStr);
            } catch (IllegalArgumentException ex) {
                return new BanOperationResult(false, "Неверный формат длительности. Пример: 5d 30m");
            }
        }

        if (isIpAddress(targetPlayer)) {
            return banIp(targetPlayer, bannedBy, permanent, expiresAt, reason);
        }

        return banPlayer(targetPlayer, bannedBy, permanent, expiresAt, reason);
    }

    private BanOperationResult banIp(
            String ipAddress,
            String bannedBy,
            boolean permanent,
            LocalDateTime expiresAt,
            String reason
    ) {
        BanRecord banRecord = BanRecord.builder()
                .ipAddress(ipAddress)
                .reason(reason)
                .bannedBy(bannedBy)
                .permanent(permanent)
                .bannedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        banRecordRepository.save(banRecord);
        playerManagerApi.findAllPlayersByIpAddress(ipAddress)
                .forEach(player -> player.kick("You have been banned from this server. \nReason: " + reason));

        return new BanOperationResult(true, "IP address " + ipAddress + " has been banned. Reason: " + reason);
    }

    private BanOperationResult banPlayer(
            String targetPlayer,
            String bannedBy,
            boolean permanent,
            LocalDateTime expiresAt,
            String reason
    ) {
        Optional<Player> optionalPlayer = playerManagerApi.findPlayer(targetPlayer, false);
        if (optionalPlayer.isEmpty()) {
            return new BanOperationResult(false, "Player not found: " + targetPlayer);
        }

        Player player = optionalPlayer.get();
        BanRecord banRecord = BanRecord.builder()
                .name(player.name())
                .playerUuid(player.uuid().toString())
                .reason(reason)
                .bannedBy(bannedBy)
                .permanent(permanent)
                .bannedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        banRecordRepository.save(banRecord);
        player.kick("You have been banned from this server. Reason: " + reason);

        return new BanOperationResult(true, "Player " + player.name() + " has been banned. Reason: " + reason);
    }

    private LocalDateTime parseExpiresAt(String durationStr) {
        LocalDateTime expiresAt = LocalDateTime.now();

        for (String part : durationStr.trim().split("\\s+")) {
            if (part.length() < 2) {
                throw new IllegalArgumentException("Invalid duration part: " + part);
            }

            long value;
            try {
                value = Long.parseLong(part.substring(0, part.length() - 1));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid duration value: " + part, ex);
            }

            char unit = Character.toLowerCase(part.charAt(part.length() - 1));
            switch (unit) {
                case 's' -> expiresAt = expiresAt.plusSeconds(value);
                case 'm' -> expiresAt = expiresAt.plusMinutes(value);
                case 'h' -> expiresAt = expiresAt.plusHours(value);
                case 'd' -> expiresAt = expiresAt.plusDays(value);
                case 'w' -> expiresAt = expiresAt.plusWeeks(value);
                default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }

        return expiresAt;
    }

    private boolean isIpAddress(String target) {
        return target.matches(IP_ADDRESS_PATTERN);
    }
}
