package irden.space.proxy.plugin.irden.ingame.bulletin_boards.model.response;

import lombok.Builder;

@Builder
public record StarboundForumPost(
        long id,
        String name,
        boolean archived,
        boolean locked,
        long createdAt, // epoch
        int messageCount
) {
}
