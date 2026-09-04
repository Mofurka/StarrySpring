package irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response;

import lombok.Builder;

@Builder
public record StarboundForumMessage(
        long id,
        String author,
        String content,
        long createdAt
) {
}
