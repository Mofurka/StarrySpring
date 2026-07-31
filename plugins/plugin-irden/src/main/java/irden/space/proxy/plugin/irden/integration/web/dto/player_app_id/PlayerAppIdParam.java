package irden.space.proxy.plugin.irden.integration.web.dto.player_app_id;


import org.jspecify.annotations.NonNull;

public record PlayerAppIdParam(long appId) {
    public static final String NAME = "appId";
    public static final String PATH = "/{" + NAME + "}";


    @Override
    public @NonNull String toString() {
        return Long.toString(appId);
    }
}