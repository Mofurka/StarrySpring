package irden.space.proxy.plugin.site.web.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LinkPlayerResponse(
        @JsonAlias("discord_id")
        Long discordId,

        @JsonAlias("application_id")
        Long applicationId
) {
}