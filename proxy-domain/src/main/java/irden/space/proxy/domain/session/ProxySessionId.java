package irden.space.proxy.domain.session;

import java.util.Objects;
import java.util.UUID;

public record ProxySessionId(UUID uuid) {

    public ProxySessionId {
        Objects.requireNonNull(uuid, "UUID cannot be null");
    }

    public static ProxySessionId generate() {
        return new ProxySessionId(UUID.randomUUID());
    }
    @Override
    public String toString() {
        return uuid.toString();
    }
}
