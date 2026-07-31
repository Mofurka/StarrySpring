package irden.space.proxy.plugin.irden.integration.web.dto.player_uuid;

import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class PlayerUuidFromStringParamConverter
        implements Converter<String, PlayerUuidParam> {

    @Override
    public PlayerUuidParam convert(@NotNull(message = "Player uuid param cannot be null") String source) {
        return new PlayerUuidParam(source);
    }
}