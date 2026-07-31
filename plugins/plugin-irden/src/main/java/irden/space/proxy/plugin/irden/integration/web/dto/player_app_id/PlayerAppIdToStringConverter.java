package irden.space.proxy.plugin.irden.integration.web.dto.player_app_id;

import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class PlayerAppIdToStringConverter
        implements Converter<PlayerAppIdParam, String> {

    @Override
    public String convert(@NotNull(message = "Player uuid param cannot be null") PlayerAppIdParam source) {
        return Long.toString(source.appId());
    }
}