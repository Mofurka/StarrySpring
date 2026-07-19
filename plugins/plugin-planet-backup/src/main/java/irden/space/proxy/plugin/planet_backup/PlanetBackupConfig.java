package irden.space.proxy.plugin.planet_backup;

import org.intellij.lang.annotations.Language;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Set;

@ConfigurationProperties(prefix = "planet-backup")
public record PlanetBackupConfig(
        boolean enabled,
        @Language("CronExp")
        String backupCron,
        Set<String> filesToExclude,
        Path storagePath,
        Path archivePath,
        int keepLast
) {

    public PlanetBackupConfig {
        if (backupCron == null || backupCron.isBlank()) {
            backupCron = "0 0 3 * * *";
        }
        if (filesToExclude == null) {
            filesToExclude = Set.of();
        }
        if (storagePath == null) {
            storagePath = Path.of("./storage/universe");
        }
        if (archivePath == null) {
            archivePath = Path.of("./storage/backups");
        }
        if (keepLast < 0) {
            keepLast = 0;
        }
    }
}
