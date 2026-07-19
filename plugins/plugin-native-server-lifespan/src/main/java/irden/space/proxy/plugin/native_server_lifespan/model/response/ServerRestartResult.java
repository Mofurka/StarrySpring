package irden.space.proxy.plugin.native_server_lifespan.model.response;

public record ServerRestartResult(
        Integer exitCode,
        Long previousPid,
        boolean wasStopped,
        boolean started,
        Long newPid
) {
}
