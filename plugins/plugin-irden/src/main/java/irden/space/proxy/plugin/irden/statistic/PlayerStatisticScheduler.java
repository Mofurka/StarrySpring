package irden.space.proxy.plugin.irden.statistic;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerStatisticScheduler {

    private final PlayerStatisticService playerStatisticService;

    @Scheduled(
            initialDelayString = "${irden.statistic.flush-interval-ms:1800000}",
            fixedDelayString = "${irden.statistic.flush-interval-ms:1800000}"
    )
    @PreDestroy
    public void flush() {
        try {
            playerStatisticService.flush();
        } catch (Exception e) {
            log.error("Не удалось слить статистику игроков в БД", e);
        }
    }
}
