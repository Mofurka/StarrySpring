package irden.space.proxy.plugin.native_server_lifespan.model.response;

public record ServerStopResult(
        Long pid,
        boolean stopped,
        Integer exitCode
) {
}
