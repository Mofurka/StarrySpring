package irden.space.proxy.plugin.command_handler;

public record CommandToken(
        String value,
        int start,
        int end
) {
}