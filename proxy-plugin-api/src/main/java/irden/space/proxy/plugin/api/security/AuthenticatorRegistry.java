package irden.space.proxy.plugin.api.security;


public interface AuthenticatorRegistry {


    AutoCloseable register(RestAuthenticator authenticator);
}
