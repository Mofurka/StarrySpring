package irden.space.proxy.protocol.payload.common.timers;

import java.util.Optional;

public record EpochTimer(
        Optional<Double> lastSeenEpochTime,
        double elapsedTime
) {
}
