package irden.space.proxy.plugin.irden.integration.web.dto.player_discord_id;

import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class PlayerDiscordIdToStringConverter
        implements Converter<PlayerDiscordIdParam, String> {

    @Override
    public String convert(@NotNull(message = "Player discord id param cannot be null") PlayerDiscordIdParam source) {
        return Long.toString(source.discordId());
    }
}