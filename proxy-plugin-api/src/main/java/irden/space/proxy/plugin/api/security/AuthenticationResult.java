package irden.space.proxy.plugin.api.security;

import java.util.Set;


public record AuthenticationResult(
        boolean authenticated,
        String principal,
        Set<String> roles,
        String failureReason
) {

    public AuthenticationResult {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static AuthenticationResult success(String principal, Set<String> roles) {
        return new AuthenticationResult(true, principal, roles, null);
    }

    public static AuthenticationResult success(String principal) {
        return success(principal, Set.of());
    }

    public static AuthenticationResult failure(String reason) {
        return new AuthenticationResult(false, null, Set.of(), reason);
    }
}
