package irden.space.proxy.plugin.runtime;

import java.util.List;

public class PluginLoader implements AutoCloseable {

    public List<PluginCandidate> loadPluginCandidates() {
        return List.of();
    }

    public void reloadPluginCandidates(List<PluginCandidate> plugins) {
        // Classpath plugin classes cannot reload their application classloader.
    }

    public void validateReloadPluginCandidates(List<PluginCandidate> plugins) {
        // Classpath plugins only recreate their managed containers.
    }

    @Override
    public void close() {
        // Classpath plugins do not own a dedicated class loader.
    }
}
