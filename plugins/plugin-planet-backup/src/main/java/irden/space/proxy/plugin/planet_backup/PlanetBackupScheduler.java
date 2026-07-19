package irden.space.proxy.plugin.planet_backup;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class PlanetBackupScheduler {

    private final PlanetBackupConfig config;
    private final PlanetBackupService backupService;

    private ScheduledExecutorService executor;
    private volatile CronExpression cron;
    private volatile boolean running;

    @OnLoad
    public void start() {
        if (!config.enabled()) {
            log.info("Planet backup disabled (planet-backup.enabled=false)");
            return;
        }

        try {
            cron = CronExpression.parse(config.backupCron());
        } catch (IllegalArgumentException e) {
            log.error("Invalid backup cron '{}'; scheduler not started", config.backupCron(), e);
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "planet-backup-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        running = true;
        scheduleNext();

        log.info("Planet backup scheduled: cron='{}', next run {}", config.backupCron(), cron.next(LocalDateTime.now()));
    }

    private void scheduleNext() {
        if (!running) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = cron.next(now);
        if (next == null) {
            log.warn("Cron '{}' has no future runs; planet backup scheduler stopped", config.backupCron());
            return;
        }

        long delayMillis = Math.max(Duration.between(now, next).toMillis(), 0);
        executor.schedule(this::runScheduledBackup, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void runScheduledBackup() {
        try {
            backupService.backupNow();
        } catch (Exception e) {
            log.error("Scheduled planet backup failed", e);
        } finally {
            scheduleNext();
        }
    }

    @OnStop
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
