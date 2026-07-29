package irden.space.proxy.plugin.irden.statistic;

import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticId;
import irden.space.proxy.plugin.irden.persistence.model.statistic.PlayerStatisticRecordEntity;
import irden.space.proxy.plugin.irden.persistence.repository.statistic.PlayerStatisticRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStatisticService {

    private final PlayerStatisticRecordRepository repository;
    private final Map<PeriodKey, Accumulator> accumulators =
            new ConcurrentHashMap<>();


    public void recordMessage(String uuid, String message) {
        if (uuid == null || message == null) {
            return;
        }

        int characters = message.length();
        int words = countWords(message);

        accumulatorFor(uuid).addMessage(words, characters);
    }


    public void recordInGameTime(String uuid, long seconds) {
        if (uuid == null || seconds <= 0) {
            return;
        }

        accumulatorFor(uuid).addSeconds(seconds);
    }

    private Accumulator accumulatorFor(String uuid) {
        LocalDate today = LocalDate.now();
        PeriodKey key = new PeriodKey(uuid, today.getYear(), today.getMonth());
        return accumulators.computeIfAbsent(key, ignored -> new Accumulator());
    }

    @Transactional
    public void flush() {
        log.info("Starting flush statistic");
        pruneEmptyPastPeriods();

        List<Runnable> onCommit = new ArrayList<>();

        accumulators.forEach((key, accumulator) -> {
            Snapshot snapshot = accumulator.peek();
            if (snapshot.isEmpty()) {
                return;
            }

            PlayerStatisticRecordEntity record = repository
                    .findById(key.toId())
                    .orElseGet(() -> PlayerStatisticRecordEntity.create(
                            key.playerUuid(), key.year(), key.month()));

            record.updateStatistic(
                    snapshot.messages(),
                    snapshot.words(),
                    snapshot.characters(),
                    snapshot.seconds()
            );
            repository.save(record);

            onCommit.add(() -> accumulator.subtract(snapshot));
        });

        if (onCommit.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        onCommit.forEach(Runnable::run);
                    }
                });
    }

    private void pruneEmptyPastPeriods() {
        LocalDate today = LocalDate.now();
        accumulators.entrySet().removeIf(entry ->
                !entry.getKey().isSamePeriod(today)
                        && entry.getValue().peek().isEmpty());
    }

    private static int countWords(String message) {
        String trimmed = message.strip();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    private record PeriodKey(String playerUuid, int year, Month month) {

        PlayerStatisticId toId() {
            return new PlayerStatisticId(playerUuid, year, month);
        }

        boolean isSamePeriod(LocalDate date) {
            return year == date.getYear() && month == date.getMonth();
        }
    }

    private record Snapshot(int messages, int words, int characters, long seconds) {

        boolean isEmpty() {
            return messages == 0
                    && words == 0
                    && characters == 0
                    && seconds == 0;
        }
    }


    private static final class Accumulator {

        private final LongAdder messages = new LongAdder();
        private final LongAdder words = new LongAdder();
        private final LongAdder characters = new LongAdder();
        private final LongAdder seconds = new LongAdder();

        void addMessage(int words, int characters) {
            this.messages.increment();
            this.words.add(words);
            this.characters.add(characters);
        }

        void addSeconds(long seconds) {
            this.seconds.add(seconds);
        }

        Snapshot peek() {
            return new Snapshot(
                    (int) messages.sum(),
                    (int) words.sum(),
                    (int) characters.sum(),
                    seconds.sum()
            );
        }

        void subtract(Snapshot snapshot) {
            messages.add(-snapshot.messages());
            words.add(-snapshot.words());
            characters.add(-snapshot.characters());
            seconds.add(-snapshot.seconds());
        }
    }
}
