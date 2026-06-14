package irden.space.proxy.plugin.ban_manager.model;

public record BanOperationResult(
        boolean success,
        String message
) {
}
