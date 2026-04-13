package irden.space.proxy.protocol.payload.common.maybe;

import java.util.Optional;

public record Maybe<T>(
        Optional<T> value
) {
}
