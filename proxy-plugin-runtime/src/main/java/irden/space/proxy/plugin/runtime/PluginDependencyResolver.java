package irden.space.proxy.plugin.runtime;



import irden.space.proxy.plugin.api.ProxyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PluginDependencyResolver {

    private static final Logger log = LoggerFactory.getLogger(PluginDependencyResolver.class);

    public List<ProxyPlugin> resolveLoadOrder(List<ProxyPlugin> plugins) {
        log.info("Resolving dependencies for {} plugin(s)", plugins.size());

        Map<String, ProxyPlugin> byId = new HashMap<>();
        for (ProxyPlugin plugin : plugins) {
            ProxyPlugin existing = byId.putIfAbsent(plugin.descriptor().id(), plugin);
            if (existing != null) {
                throw new IllegalStateException("Duplicate plugin id '" + plugin.descriptor().id() + "'");
            }
        }

        for (ProxyPlugin plugin : plugins) {
            log.info(
                    "Plugin '{}' declares dependencies: {}",
                    plugin.descriptor().id(),
                    formatDependencies(plugin.descriptor().dependsOn())
            );

            for (String dependency : plugin.descriptor().dependsOn()) {
                if (!byId.containsKey(dependency)) {
                    log.error(
                            "Plugin '{}' requires missing dependency '{}'. Declared dependencies: {}",
                            plugin.descriptor().id(),
                            dependency,
                            formatDependencies(plugin.descriptor().dependsOn())
                    );
                    throw new IllegalStateException(
                            "Plugin '" + plugin.descriptor().id() + "' depends on missing plugin '" + dependency + "'"
                    );
                }
            }
        }

        List<ProxyPlugin> result = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (ProxyPlugin plugin : plugins) {
            visit(plugin, byId, visiting, visited, result);
        }

        log.info(
                "Resolved plugin dependency order: {}",
                result.stream()
                        .map(plugin -> plugin.descriptor().id())
                        .toList()
        );

        return result;
    }

    public List<PluginCandidate> resolveCandidateLoadOrder(List<PluginCandidate> plugins) {
        log.info("Resolving dependencies for {} plugin candidate(s)", plugins.size());

        Map<String, PluginCandidate> byId = new HashMap<>();
        for (PluginCandidate plugin : plugins) {
            PluginCandidate existing = byId.putIfAbsent(plugin.descriptor().id(), plugin);
            if (existing != null) {
                throw new IllegalStateException("Duplicate plugin id '" + plugin.descriptor().id() + "'");
            }
        }

        for (PluginCandidate plugin : plugins) {
            for (String dependency : plugin.descriptor().dependsOn()) {
                if (!byId.containsKey(dependency)) {
                    throw new IllegalStateException(
                            "Plugin '" + plugin.descriptor().id() + "' depends on missing plugin '" + dependency + "'"
                    );
                }
            }
        }

        List<PluginCandidate> result = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (PluginCandidate plugin : plugins) {
            visitCandidate(plugin, byId, visiting, visited, result);
        }
        return result;
    }

    private void visitCandidate(
            PluginCandidate plugin,
            Map<String, PluginCandidate> byId,
            Set<String> visiting,
            Set<String> visited,
            List<PluginCandidate> result
    ) {
        String id = plugin.descriptor().id();
        if (visited.contains(id)) {
            return;
        }
        if (!visiting.add(id)) {
            throw new IllegalStateException("Cyclic plugin dependency detected at plugin '" + id + "'");
        }
        for (String dependency : plugin.descriptor().dependsOn()) {
            visitCandidate(byId.get(dependency), byId, visiting, visited, result);
        }
        visiting.remove(id);
        visited.add(id);
        result.add(plugin);
    }

    private void visit(
            ProxyPlugin plugin,
            Map<String, ProxyPlugin> byId,
            Set<String> visiting,
            Set<String> visited,
            List<ProxyPlugin> result
    ) {
        String id = plugin.descriptor().id();

        if (visited.contains(id)) {
            return;
        }

        if (visiting.contains(id)) {
            log.error("Cyclic plugin dependency detected at plugin '{}'", id);
            throw new IllegalStateException("Cyclic plugin dependency detected at plugin '" + id + "'");
        }

        visiting.add(id);
        log.debug("Visiting plugin '{}' with dependencies {}", id, formatDependencies(plugin.descriptor().dependsOn()));

        for (String dep : plugin.descriptor().dependsOn()) {
            visit(byId.get(dep), byId, visiting, visited, result);
        }

        visiting.remove(id);
        visited.add(id);
        result.add(plugin);
    }

    private String formatDependencies(List<String> dependencies) {
        return dependencies == null || dependencies.isEmpty() ? "[]" : dependencies.toString();
    }
}
