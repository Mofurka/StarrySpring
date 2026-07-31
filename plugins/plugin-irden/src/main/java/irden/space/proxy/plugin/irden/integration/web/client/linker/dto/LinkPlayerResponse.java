package irden.space.proxy.plugin.irden.integration.web.client.linker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LinkPlayerResponse(
        @JsonAlias("discord_id")
        Long discordId,

        @JsonAlias("application_id")
        Long applicationId
) {
}