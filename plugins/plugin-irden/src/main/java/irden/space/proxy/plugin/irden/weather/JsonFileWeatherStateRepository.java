package irden.space.proxy.plugin.irden.weather;

import irden.space.proxy.plugin.irden.IrdenConfig;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Repository
public class JsonFileWeatherStateRepository implements WeatherStateRepository {

    private final JsonMapper objectMapper;
    private final Path statePath;

    public JsonFileWeatherStateRepository(
            JsonMapper objectMapper,
            IrdenConfig properties
    ) {
        this.objectMapper = objectMapper;
        this.statePath = properties.weather().statePath().toAbsolutePath().normalize();
    }

    @Override
    public synchronized Optional<WeatherRuntimeState> load() {
        if (!Files.exists(statePath)) {
            return Optional.empty();
        }

        return Optional.of(
                objectMapper.readValue(statePath.toFile(), WeatherRuntimeState.class)
        );
    }

    @Override
    public synchronized void save(WeatherRuntimeState state) {
        try {
            Path parent = statePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temporary = statePath.resolveSibling(statePath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), state);

            try {
                Files.move(
                        temporary,
                        statePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException _) {
                Files.move(
                        temporary,
                        statePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save weather runtime state: " + statePath,
                    e
            );
        }
    }
}
