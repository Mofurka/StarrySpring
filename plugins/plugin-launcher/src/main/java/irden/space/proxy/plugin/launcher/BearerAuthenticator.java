package irden.space.proxy.plugin.launcher;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.security.AuthenticationRequest;
import irden.space.proxy.plugin.api.security.AuthenticationResult;
import irden.space.proxy.plugin.api.security.AuthenticatorRegistry;
import irden.space.proxy.plugin.api.security.RestAuthenticator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
public class BearerAuthenticator implements RestAuthenticator {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final LauncherConfig config;
    private final AuthenticatorRegistry authenticatorRegistry;

    private AutoCloseable registration;

    @OnLoad
    public void register() {
        if (config.bearerToken() == null || config.bearerToken().isBlank()) {
            log.warn("launcher.bearer-token is empty — bearer auth will reject every request (stub)");
        }
        registration = authenticatorRegistry.register(this);
        log.info("Registered launcher bearer authenticator");
    }

    @OnStop
    public void unregister() {
        if (registration != null) {
            try {
                registration.close();
            } catch (Exception e) {
                log.debug("Failed to unregister bearer authenticator", e);
            }
        }
    }

    @Override
    public boolean supports(AuthenticationRequest request) {
        String header = request.header(AUTHORIZATION_HEADER);
        return header != null && header.startsWith(BEARER_PREFIX);
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        String header = request.header(AUTHORIZATION_HEADER);
        String token = header.substring(BEARER_PREFIX.length()).trim();

        // TODO: заменить на реальную валидацию токена
        String expected = config.bearerToken();
        if (expected == null || expected.isBlank() || !constantTimeEquals(expected, token)) {
            return AuthenticationResult.failure("Invalid bearer token");
        }

        return AuthenticationResult.success("launcher", Set.of("ROLE_LAUNCHER"));
    }

    private static boolean constantTimeEquals(String expected, String presented) {
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8)
        );
    }
}
