package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.runtime.fixture.ExternalTestPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.nio.file.Files.newOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class Pf4jPluginLoaderTest {

    @TempDir
    Path pluginsDirectory;

    @Test
    void reloadsExternalPluginWithNewClassLoader() throws IOException {
        createPluginJar(pluginsDirectory.resolve("external-test.jar"));
        Pf4jPluginLoader loader = new Pf4jPluginLoader(pluginsDirectory);

        PluginCandidate firstCandidate = findExternalPlugin(loader.loadPluginCandidates());
        ClassLoader firstClassLoader = firstCandidate.pluginClass().getClassLoader();

        loader.reloadPluginCandidates(List.of(firstCandidate));

        PluginCandidate secondCandidate = findExternalPlugin(loader.loadPluginCandidates());
        assertEquals("external-test", secondCandidate.descriptor().id());
        assertNotSame(firstClassLoader, secondCandidate.pluginClass().getClassLoader());

        loader.close();
    }

    private PluginCandidate findExternalPlugin(List<PluginCandidate> plugins) {
        return plugins.stream()
                .filter(plugin -> "external-test".equals(plugin.descriptor().id()))
                .findFirst()
                .orElseThrow();
    }

    private void createPluginJar(Path jarPath) throws IOException {
        String pluginClassName = ExternalTestPlugin.class.getName();
        String pluginClassPath = pluginClassName.replace('.', '/') + ".class";

        try (JarOutputStream jar = new JarOutputStream(newOutputStream(jarPath))) {
            writeEntry(
                    jar,
                    "plugin.properties",
                    """
                    plugin.id=external-test-container
                    plugin.version=1.0.0
                    """
                            .getBytes(StandardCharsets.UTF_8)
            );
            writeEntry(
                    jar,
                    "META-INF/extensions.idx",
                    (pluginClassName + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)
            );

            try (InputStream classBytes = ExternalTestPlugin.class.getClassLoader().getResourceAsStream(pluginClassPath)) {
                if (classBytes == null) {
                    throw new IllegalStateException("Cannot find test plugin class bytes");
                }
                writeEntry(jar, pluginClassPath, classBytes.readAllBytes());
            }
        }
    }

    private void writeEntry(JarOutputStream jar, String name, byte[] contents) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        jar.write(contents);
        jar.closeEntry();
    }
}
