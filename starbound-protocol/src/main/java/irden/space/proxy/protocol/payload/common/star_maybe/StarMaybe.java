package irden.space.proxy.protocol.payload.common.star_maybe;

import java.util.Optional;

public record StarMaybe<T>(
        Optional<T> value
) {
}
