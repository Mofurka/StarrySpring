package irden.space.proxy.plugin.planet_backup;

import irden.space.proxy.plugin.native_server_lifespan.ServerLifespan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Component
@RequiredArgsConstructor
@Slf4j
public class PlanetBackupService {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final PlanetBackupConfig config;
    private final ServerLifespan serverLifespan;
    private final ReentrantLock lock = new ReentrantLock();


    public Path backupNow() throws IOException {
        if (!lock.tryLock()) {
            throw new IllegalStateException("A planet backup is already in progress");
        }
        try {
            return runBackup();
        } finally {
            lock.unlock();
        }
    }

    private Path runBackup() throws IOException {
        Path source = resolveAgainstGameDirectory(config.storagePath());
        if (!Files.isDirectory(source)) {
            throw new IOException("Universe storage path is not a directory: " + source);
        }

        boolean serverWasRunning = serverLifespan.getServerPid() != null;
        log.info("Starting planet backup (server managed & running: {})", serverWasRunning);

        if (serverWasRunning) {
            serverLifespan.stopServer();
        }

        Path archive;
        try {
            archive = createArchive(source);
            log.info("Planet backup written to {} ({} KiB)", archive, Files.size(archive) / 1024);
        } finally {
            if (serverWasRunning) {
                serverLifespan.startServer();
            }
        }

        pruneOldArchives();
        return archive;
    }

    private Path createArchive(Path source) throws IOException {
        Path archiveDir = resolveAgainstGameDirectory(config.archivePath());
        Files.createDirectories(archiveDir);
        Path archive = archiveDir.resolve("universe-" + TIMESTAMP.format(Instant.now()) + ".zip");

        try (ZipOutputStream zip = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(archive)))) {
            try (Stream<Path> tree = Files.walk(source)) {
                List<Path> files = tree
                        .filter(Files::isRegularFile)
                        .filter(path -> !isExcluded(path))
                        .toList();

                for (Path file : files) {
                    writeEntry(zip, source, file);
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(archive); // не оставляем битый архив
            throw e;
        }

        return archive;
    }

    private void writeEntry(ZipOutputStream zip, Path source, Path file) throws IOException {
        String entryName = source.relativize(file).toString().replace('\\', '/');
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    private boolean isExcluded(Path file) {
        String fileName = file.getFileName().toString();
        return config.filesToExclude().stream().anyMatch(fileName::startsWith);
    }

    private void pruneOldArchives() {
        int keep = config.keepLast();
        if (keep <= 0) {
            return;
        }

        Path archiveDir = resolveAgainstGameDirectory(config.archivePath());
        try (Stream<Path> archives = Files.list(archiveDir)) {
            List<Path> sortedNewestFirst = archives
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(PlanetBackupService::lastModifiedMillis).reversed())
                    .toList();

            for (int i = keep; i < sortedNewestFirst.size(); i++) {
                Path old = sortedNewestFirst.get(i);
                Files.deleteIfExists(old);
                log.info("Pruned old planet backup {}", old.getFileName());
            }
        } catch (IOException e) {
            log.warn("Failed to prune old planet backups in {}", archiveDir, e);
        }
    }

    /**
     * Относительные пути резолвятся относительно game-directory из lifespan-плагина
     * (там лежит папка universe), абсолютные - как есть. Если game-directory ещё не
     * инициализирована - фолбэк на рабочую директорию процесса.
     */
    private Path resolveAgainstGameDirectory(Path path) {
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path gameDirectory = serverLifespan.gameDirectory();
        Path base = gameDirectory != null ? gameDirectory : Path.of("").toAbsolutePath();
        return base.resolve(path).normalize();
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
