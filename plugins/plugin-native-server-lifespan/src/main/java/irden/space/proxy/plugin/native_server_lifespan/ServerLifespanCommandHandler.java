package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.native_server_lifespan.model.response.NativeServerInfo;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerRestartResult;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerStartResult;
import irden.space.proxy.plugin.native_server_lifespan.model.response.ServerStopResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(
        value = "native-server-lifespan.enabled",
        matchIfMissing = true
)
public class ServerLifespanCommandHandler {
    private final ServerLifespan serverLifespan;


    public void handleCommand(CommandContext commandContext) {
        var argument = commandContext.get("argument", ServerLifespanCommands.NativeServerCommandEnumArgument.class);
        StringBuilder sb = new StringBuilder("Native server command result:");
        sb.append(System.lineSeparator());
        switch (argument) {
            case STOP -> {
                ServerStopResult serverStopResult = serverLifespan.stopServer();
                sb.append("- Server stopped: %s".formatted(serverStopResult.stopped()));
                sb.append(System.lineSeparator());
                sb.append("- PID %s".formatted(serverStopResult.pid()));
                sb.append(System.lineSeparator());
                sb.append("- Exit code %s".formatted(serverStopResult.exitCode()));
            }
            case START -> {
                ServerStartResult serverStartResult = serverLifespan.startServer();
                sb.append("- started: %s".formatted(serverStartResult.success()));
                sb.append(System.lineSeparator());
                sb.append("- PID: %s".formatted(serverStartResult.pid()));
            }
            case RESTART -> {
                ServerRestartResult serverRestartResult = serverLifespan.restartServer();
                sb.append("- restarted: %s".formatted(serverRestartResult.started()));
                sb.append(System.lineSeparator());
                sb.append("- previous PID: %s".formatted(serverRestartResult.previousPid()));
                sb.append(System.lineSeparator());
                sb.append("- exitCode: %s".formatted(serverRestartResult.exitCode()));
                sb.append(System.lineSeparator());
                sb.append("- old instance stopped: %s".formatted(serverRestartResult.wasStopped()));
                sb.append(System.lineSeparator());
                sb.append("- new PID: %s".formatted(serverRestartResult.newPid()));
            }
            case INFO -> {
                NativeServerInfo info = serverLifespan.getServerInfo();

                sb.append("- Running: %s".formatted(info.running()));

                if (info.running()) {
                    sb.append(System.lineSeparator());
                    sb.append("- PID: %s".formatted(info.pid()));

                    sb.append(System.lineSeparator());
                    sb.append("- Started at: %s".formatted(info.startedAt()));

                    sb.append(System.lineSeparator());
                    sb.append("- Uptime: %s".formatted(formatDuration(info.uptime())));

                    sb.append(System.lineSeparator());
                    sb.append("- CPU time: %s".formatted(formatDuration(info.totalCpuTime())));

                    sb.append(System.lineSeparator());
                    sb.append("- User: %s".formatted(info.user()));

                    sb.append(System.lineSeparator());
                    sb.append("- Command: %s".formatted(info.command()));

                    sb.append(System.lineSeparator());
                    sb.append("- Arguments: %s".formatted(info.arguments()));
                }
            }
        }
        commandContext.reply(sb.toString().trim());
    }
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "unknown";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return "%dd %02d:%02d:%02d"
                    .formatted(days, hours, minutes, seconds);
        }

        return "%02d:%02d:%02d"
                .formatted(hours, minutes, seconds);
    }
}
