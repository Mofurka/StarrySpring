package irden.space.proxy.plugin.native_server_lifespan.model.response;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record NativeServerInfo(
        boolean running,
        Long pid,
        String command,
        String commandLine,
        List<String> arguments,
        String user,
        Instant startedAt,
        Duration uptime,
        Duration totalCpuTime
) {

    public static NativeServerInfo stopped() {
        return new NativeServerInfo(
                false,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null
        );
    }
}