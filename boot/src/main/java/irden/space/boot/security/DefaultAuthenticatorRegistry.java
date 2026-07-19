package irden.space.boot.security;

import irden.space.proxy.plugin.api.security.AuthenticatorRegistry;
import irden.space.proxy.plugin.api.security.RestAuthenticator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class DefaultAuthenticatorRegistry implements AuthenticatorRegistry {

    private final CopyOnWriteArrayList<RestAuthenticator> authenticators = new CopyOnWriteArrayList<>();

    @Override
    public AutoCloseable register(RestAuthenticator authenticator) {
        Objects.requireNonNull(authenticator, "authenticator");
        authenticators.addIfAbsent(authenticator);
        return () -> authenticators.remove(authenticator);
    }

    public List<RestAuthenticator> ordered() {
        return authenticators.stream()
                .sorted(Comparator.comparingInt(RestAuthenticator::order))
                .toList();
    }
}
