package irden.space.proxy.plugin.star_custom_chat.model;

public record StarCustomChatEntityMessage(
        String mode,
        Integer connection,
        String text,
        String nickname
) {
}
