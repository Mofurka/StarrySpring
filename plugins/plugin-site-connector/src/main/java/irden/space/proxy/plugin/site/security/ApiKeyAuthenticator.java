package irden.space.proxy.plugin.site.security;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.security.AuthenticationRequest;
import irden.space.proxy.plugin.api.security.AuthenticationResult;
import irden.space.proxy.plugin.api.security.AuthenticatorRegistry;
import irden.space.proxy.plugin.api.security.RestAuthenticator;
import irden.space.proxy.plugin.site.persistence.config.SiteConnectorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticator implements RestAuthenticator {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private final SiteConnectorConfig config;
    private final AuthenticatorRegistry authenticatorRegistry;

    private AutoCloseable registration;

    @OnLoad
    public void register() {
        if (config.inboundApiKey() == null || config.inboundApiKey().isBlank()) {
            log.warn("site-connector.inbound-api-key is empty — inbound API-key auth will reject every request");
        }
        registration = authenticatorRegistry.register(this);
        log.info("Registered inbound API-key authenticator");
    }

    @OnStop
    public void unregister() {
        if (registration != null) {
            try {
                registration.close();
            } catch (Exception e) {
                log.debug("Failed to unregister API-key authenticator", e);
            }
        }
    }

    @Override
    public boolean supports(AuthenticationRequest request) {
        return request.header(API_KEY_HEADER) != null;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        String expected = config.inboundApiKey();
        String presented = request.header(API_KEY_HEADER);

        if (expected == null || expected.isBlank() || !constantTimeEquals(expected, presented)) {
            return AuthenticationResult.failure("Invalid API key");
        }

        return AuthenticationResult.success("site-integration", Set.of("ROLE_SITE"));
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
