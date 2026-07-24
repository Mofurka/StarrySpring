package irden.space.proxy.plugin.irden.weather;

import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.irden.IrdenConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class WeatherConfigService {

    private final JsonMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final IrdenConfig.WeatherProperties properties;
    private final WeatherConfigValidator validator;
    private final AtomicReference<WeatherConfig> current = new AtomicReference<>();

    public WeatherConfigService(
            JsonMapper objectMapper,
            ResourceLoader resourceLoader,
            IrdenConfig properties,
            WeatherConfigValidator validator
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.properties = properties.weather();
        this.validator = validator;
    }

    @OnLoad
    public void initialize() {
        reload();
    }

    public WeatherConfig get() {
        WeatherConfig config = current.get();
        if (config == null) {
            throw new IllegalStateException("Weather configuration has not been loaded");
        }
        return config;
    }

    public synchronized WeatherConfig reload() {
        Resource resource = resourceLoader.getResource(properties.configLocation());

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Weather config does not exist: " + properties.configLocation()
            );
        }

        try (InputStream input = resource.getInputStream()) {
            WeatherConfig loaded = objectMapper.readValue(input, WeatherConfig.class);
            validator.validate(loaded);
            current.set(loaded);
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read weather config: " + properties.configLocation(),
                    e
            );
        }
    }
}
