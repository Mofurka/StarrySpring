package irden.space.proxy.plugin.star_custom_chat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IrdenCustomChatSH(
        String message,
        IrdenCustomChatSHData data
) {
}
