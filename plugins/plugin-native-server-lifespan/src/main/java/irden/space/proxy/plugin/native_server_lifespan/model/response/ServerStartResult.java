package irden.space.proxy.plugin.native_server_lifespan.model.response;

public record ServerStartResult(
        Long pid,
        boolean success
) {
}
