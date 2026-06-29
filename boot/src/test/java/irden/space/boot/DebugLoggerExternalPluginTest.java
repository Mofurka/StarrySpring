package irden.space.boot;

import irden.space.proxy.plugin.runtime.Pf4jPluginLoader;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DebugLoggerExternalPluginTest {

    private static final String PLUGIN_ID = "debug-logger";
    private static final String PLUGIN_CLASS_NAME = "irden.space.proxy.plugin.debug.DebugLoggerPlugin";

    @Test
    void discoversAndReloadsDebugLoggerAsExternalPlugin(@TempDir Path pluginsDirectory) throws IOException {
        Path builtJar = locateDebugLoggerJar().orElse(null);
        assumeTrue(builtJar != null, "debug-logger distribution JAR is not built; run 'mvn package'/'install'");

        Files.copy(builtJar, pluginsDirectory.resolve("plugin-debug-logger.jar"));
        Pf4jPluginLoader loader = new Pf4jPluginLoader(pluginsDirectory);

        PluginCandidate firstCandidate = findDebugLogger(loader.loadPluginCandidates());
        assertEquals(PLUGIN_ID, firstCandidate.descriptor().id());
        assertEquals(PLUGIN_CLASS_NAME, firstCandidate.pluginClass().getName());
        // Not on the host classpath, so the class is loaded by an isolated PF4J classloader.
        assertNotSame(getClass().getClassLoader(), firstCandidate.pluginClass().getClassLoader());
        ClassLoader firstClassLoader = firstCandidate.pluginClass().getClassLoader();

        loader.reloadPluginCandidates(List.of(firstCandidate));

        PluginCandidate secondCandidate = findDebugLogger(loader.loadPluginCandidates());
        assertEquals(PLUGIN_ID, secondCandidate.descriptor().id());
        assertNotSame(firstClassLoader, secondCandidate.pluginClass().getClassLoader());

        loader.close();
    }

    private PluginCandidate findDebugLogger(List<PluginCandidate> plugins) {
        return plugins.stream()
                .filter(plugin -> PLUGIN_ID.equals(plugin.descriptor().id()))
                .findFirst()
                .orElseThrow();
    }

    private Optional<Path> locateDebugLoggerJar() throws IOException {
        // Surefire runs with the module directory (boot/) as the working directory.
        Path targetDir = Path.of("..", "plugins", "plugin-debug-logger", "target");
        if (!Files.isDirectory(targetDir)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(targetDir, "plugin-debug-logger-*.jar")) {
            for (Path jar : jars) {
                String name = jar.getFileName().toString();
                if (!name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar")) {
                    return Optional.of(jar);
                }
            }
        }
        return Optional.empty();
    }
}
