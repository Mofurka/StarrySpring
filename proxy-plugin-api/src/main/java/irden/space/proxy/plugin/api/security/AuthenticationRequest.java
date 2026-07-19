package irden.space.proxy.plugin.api.security;


public interface AuthenticationRequest {

    String method();

    String path();

    String header(String name);
}
