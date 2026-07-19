package irden.space.proxy.plugin.api.security;


public interface RestAuthenticator {

    boolean supports(AuthenticationRequest request);

    AuthenticationResult authenticate(AuthenticationRequest request);

    default int order() {
        return 0;
    }
}
