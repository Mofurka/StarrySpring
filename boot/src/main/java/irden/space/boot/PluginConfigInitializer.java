package irden.space.boot;

import irden.space.proxy.plugin.runtime.PluginCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;


final class PluginConfigInitializer {

    private static final Logger log = LoggerFactory.getLogger(PluginConfigInitializer.class);

    private static final String CONFIG_DIR_PROPERTY = "starry.plugins.config-directory";
    private static final String DEFAULT_CONFIG_DIR = "config/plugins";

    private static final String DEFAULT_CONFIG_RESOURCE_TEMPLATE = "config/%s.yaml";

    private final ApplicationContext rootContext;
    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    PluginConfigInitializer(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    void apply(AnnotationConfigApplicationContext pluginContext, PluginCandidate candidate) {
        String pluginId = candidate.descriptor().id();
        Path configFile = resolveConfigFile(pluginId);

        try {
            ensureConfigFile(configFile, candidate);
        } catch (IOException e) {
            log.warn("Failed to prepare config file {} for plugin '{}'", configFile, pluginId, e);
            return;
        }

        if (!Files.isRegularFile(configFile)) {
            // Ни внешнего файла, ни дефолта в ресурсах — плагин полагается на значения по умолчанию
            return;
        }

        addPropertySource(pluginContext, pluginId, configFile);
    }

    private Path resolveConfigFile(String pluginId) {
        String directory = rootContext.getEnvironment().getProperty(CONFIG_DIR_PROPERTY, DEFAULT_CONFIG_DIR);
        return Path.of(directory, pluginId + ".yaml");
    }

    private void ensureConfigFile(Path configFile, PluginCandidate candidate) throws IOException {
        if (Files.exists(configFile)) {
            return;
        }

        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String resourceName = DEFAULT_CONFIG_RESOURCE_TEMPLATE.formatted(candidate.descriptor().id());
        ClassLoader pluginClassLoader = candidate.pluginClass().getClassLoader();
        try (InputStream defaults = pluginClassLoader.getResourceAsStream(resourceName)) {
            if (defaults == null) {
                return;
            }
            Files.copy(defaults, configFile);
            log.info("Created default config for plugin '{}' at {}", candidate.descriptor().id(), configFile);
        }
    }

    private void addPropertySource(AnnotationConfigApplicationContext pluginContext, String pluginId, Path configFile) {
        try {
            List<PropertySource<?>> sources = yamlLoader.load(
                    "plugin-config[" + pluginId + "]",
                    new FileSystemResource(configFile)
            );

            MutablePropertySources target = pluginContext.getEnvironment().getPropertySources();
            for (int i = sources.size() - 1; i >= 0; i--) {
                target.addFirst(sources.get(i));
            }

            log.info("Loaded config for plugin '{}' from {}", pluginId, configFile);
        } catch (IOException e) {
            log.warn("Failed to load config {} for plugin '{}'", configFile, pluginId, e);
        }
    }
}
