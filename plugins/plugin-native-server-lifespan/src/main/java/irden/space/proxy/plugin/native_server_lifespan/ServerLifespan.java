package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.native_server_lifespan.model.response.NativeServerInfo;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerRestartResult;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerStopResult;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerStartResult;
import irden.space.proxy.plugin.native_server_lifespan.rcon.RconAuthenticationException;
import irden.space.proxy.plugin.native_server_lifespan.rcon.StarboundRconClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(
        value = "native-server-lifespan.enabled",
        matchIfMissing = true
)
public class ServerLifespan {
    private final NativeServerLifespanConfig config;
    // RCON-клиент опционален: при native-server-lifespan.rcon.enabled=false бин не создаётся.
    private final ObjectProvider<StarboundRconClient> rconClientProvider;
    private Path gameDirectoryPath;
    private Path gameExecutablePath;

    private volatile Process serverProcess;

    @OnLoad
    public void onLoad(PluginContext context) {
        this.resolvePaths();
        this.validatePaths();
        this.startServer();
    }

    private void resolvePaths() {
        Path configuredPath = config.gameDirectoryPath();

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();

        gameDirectoryPath = configuredPath.isAbsolute() ? configuredPath.normalize() : workingDirectory.resolve(configuredPath).normalize();

        log.info("Configured game directory: {}", configuredPath);
        log.info("Resolved game directory: {}", gameDirectoryPath);

        Path relativeExecutablePath = switch (OperatingSystem.current()) {
            case LINUX -> {
                log.info("Running on Linux");
                yield Path.of("linux", "starbound_server");
            }
            case WINDOWS -> {
                log.info("Running on Windows");
                yield Path.of("win64", "starbound_server.exe");
            }
            default ->
                    throw new IllegalStateException("Unsupported operating system: " + System.getProperty("os.name"));
        };


        gameExecutablePath = gameDirectoryPath.resolve(relativeExecutablePath).normalize();
        log.info("Resolved game executable path: {}", gameExecutablePath);
    }

    private void validatePaths() {
        if (!Files.isDirectory(gameDirectoryPath)) {
            throw new IllegalStateException("Game directory does not exist or is not a directory: " + gameDirectoryPath);
        }

        if (!Files.isRegularFile(gameExecutablePath)) {
            throw new IllegalStateException("Game executable does not exist or is not a regular file: " + gameExecutablePath);
        }

        if (OperatingSystem.current() == OperatingSystem.LINUX && !Files.isExecutable(gameExecutablePath)) {
            throw new IllegalStateException("Game executable is not executable: " + gameExecutablePath + ". Run: chmod +x " + gameExecutablePath);
        }
    }

    public synchronized ServerStartResult startServer() {
        if (isServerRunning()) {
            throw new IllegalStateException("Server is already running, PID: " + serverProcess.pid());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(gameExecutablePath.toString());
        processBuilder.directory(gameExecutablePath.getParent().toFile());

        processBuilder.redirectErrorStream(true);

        try {
            serverProcess = processBuilder.start();

            log.info("Server started, PID: {}", serverProcess.pid());
            startOutputReader(serverProcess);
            startExitWatcher(serverProcess);
            return new ServerStartResult(getServerPid(), true);
        } catch (IOException e) {
            serverProcess = null;
            throw new IllegalStateException("Failed to start server: " + e.getMessage(), e);
        }

    }

    public synchronized ServerStopResult stopServer() {
        Process process = serverProcess;

        if (process == null || !process.isAlive()) {
            log.info("Game server is not running");
            serverProcess = null;
            return new ServerStopResult(null, false, null);
        }

        log.info(
                "Stopping game server gracefully, PID: {}",
                process.pid()
        );

        StarboundRconClient rconClient = rconClientProvider.getIfAvailable();
        if (rconClient != null) {
            try {
                rconClient.stopServer();

                boolean stopped = process.waitFor(
                        config.rcon().shutdownTimeout().toMillis(),
                        TimeUnit.MILLISECONDS
                );

                if (stopped) {
                    log.info(
                            "Game server stopped gracefully, PID: {}, exit code: {}",
                            process.pid(),
                            process.exitValue()
                    );

                    serverProcess = null;
                    return new ServerStopResult(process.pid(), true, process.exitValue());
                }

                log.warn(
                        "Game server did not stop after RCON command within {}",
                        config.rcon().shutdownTimeout()
                );
            } catch (RconAuthenticationException e) {
                log.error("Invalid RCON password", e);
            } catch (IOException e) {
                log.error("Unable to send RCON stop command", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for game server shutdown", e);
            }
        } else {
            log.warn("RCON is disabled; terminating game server process directly");
        }

        boolean forciblyStopped = forceStopServer(process);
        Integer exitCode = process.isAlive() ? null : process.exitValue();
        ServerStopResult serverStopResult = new ServerStopResult(process.pid(), forciblyStopped, exitCode);
        serverProcess = null;
        return serverStopResult;
    }

    private boolean forceStopServer(Process process) {
        log.warn(
                "Falling back to process termination, PID: {}",
                process.pid()
        );

        process.destroy();

        try {
            if (process.waitFor(5, TimeUnit.SECONDS)) {
                return true;
            }

            log.warn(
                    "Process ignored destroy(), forcing termination, PID: {}",
                    process.pid()
            );

            process.destroyForcibly();

            if (process.waitFor(5, TimeUnit.SECONDS)) {
                return true;
            }

            log.error(
                    "Unable to terminate game server, PID: {}",
                    process.pid()
            );
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return false;
        }
    }

    public synchronized ServerRestartResult restartServer() {
        Long previousPid = null;
        Integer exitCode = null;
        Boolean stopped = null;
        if (isServerRunning()) {
            ServerStopResult serverStopResult = stopServer();
            previousPid = serverStopResult.pid();
            exitCode = serverStopResult.exitCode();
            stopped = serverStopResult.stopped();
        }
        ServerStartResult serverStartResult = startServer();
        return new ServerRestartResult(
                exitCode,
                previousPid,
                Boolean.TRUE.equals(stopped),
                serverStartResult.success(),
                serverStartResult.pid()
        );
    }

    private void startOutputReader(Process process) {
        Thread.ofVirtual()
                .name("game-server-output_" + process.pid())
                .start(
                        () -> {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream(), StandardCharsets.UTF_8
                                    )
                            )) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    log.info("[Game server] {}", line);
                                }

                            } catch (Exception e) {
                                if (process.isAlive()) {
                                    log.error("Failed to start server output", e);
                                }
                            }
                        }
                );
    }

    private void startExitWatcher(Process process) {
        process.onExit().thenAccept(exitProcess -> {
            int exitValue = exitProcess.exitValue();
            if (exitValue == 0) {
                log.info("Game server stopped normally, PID: {}", exitProcess.pid());
            } else {
                log.warn("Game server stopped unexpectedly, PID: {}, exit code: {}", exitProcess.pid(), exitValue);
            }

            synchronized (this) {
                if (serverProcess == exitProcess) {
                    serverProcess = null;
                }
            }
        });
    }

    private boolean isServerRunning() {
        Process process = serverProcess;
        return process != null && process.isAlive();
    }

    public Long getServerPid() {
        if (!isServerRunning()) {
            return null;
        }
        Process process = serverProcess;

        return process.pid();
    }

    /**
     * Резолвнутая игровая директория (задаётся в {@code @OnLoad}). Другие плагины могут
     * резолвить свои относительные пути относительно неё. {@code null}, если ещё не инициализирована.
     */
    public Path gameDirectory() {
        return gameDirectoryPath;
    }

    public NativeServerInfo getServerInfo() {
        Process process = serverProcess;

        if (process == null || !process.isAlive()) {
            return NativeServerInfo.stopped();
        }

        ProcessHandle processHandle = process.toHandle();
        ProcessHandle.Info processInfo = processHandle.info();

        Instant startedAt = processInfo
                .startInstant()
                .orElse(null);

        Duration uptime = startedAt == null
                ? null
                : Duration.between(startedAt, Instant.now());

        List<String> arguments = processInfo
                .arguments()
                .map(values -> List.copyOf(Arrays.asList(values)))
                .orElseGet(List::of);

        return new NativeServerInfo(
                true,
                processHandle.pid(),
                processInfo.command().orElse(null),
                processInfo.commandLine().orElse(null),
                arguments,
                processInfo.user().orElse(null),
                startedAt,
                uptime,
                processInfo.totalCpuDuration().orElse(null)
        );
    }


    @OnStop
    @PreDestroy
    public void destroy() {
        stopServer();
    }

}
