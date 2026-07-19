package irden.space.proxy.plugin.native_server_lifespan.rcon;

import java.io.IOException;

public class RconAuthenticationException extends IOException {

    public RconAuthenticationException(String message) {
        super(message);
    }
}