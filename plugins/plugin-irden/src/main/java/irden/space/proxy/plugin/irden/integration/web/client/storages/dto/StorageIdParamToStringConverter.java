package irden.space.proxy.plugin.irden.integration.web.client.storages.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StorageIdParamToStringConverter
        implements Converter<StorageIdParam, String> {

    @Override
    public String convert(@NotNull(message = "Player uuid param cannot be null") StorageIdParam source) {
        return Long.toString(source.storageId());
    }
}
