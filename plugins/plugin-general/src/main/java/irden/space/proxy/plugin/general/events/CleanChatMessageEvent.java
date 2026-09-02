package irden.space.proxy.plugin.general.events;

import java.util.Map;

public record CleanChatMessageEvent(
        String sender,
        String mode,
        String message,
        Map<String, Object> metadata
) {
    public CleanChatMessageEvent(String sender, String mode, String message) {
        this(sender, mode, message, null);
    }
}
