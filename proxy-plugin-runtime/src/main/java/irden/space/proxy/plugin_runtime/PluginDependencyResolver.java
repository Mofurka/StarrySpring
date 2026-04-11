package irden.space.proxy.plugin_runtime;



import irden.space.proxy.plugin_api.ProxyPlugin;

import java.util.*;

public class PluginDependencyResolver {

    public List<ProxyPlugin> resolveLoadOrder(List<ProxyPlugin> plugins) {
        Map<String, ProxyPlugin> byId = new HashMap<>();
        for (ProxyPlugin plugin : plugins) {
            byId.put(plugin.descriptor().id(), plugin);
        }

        for (ProxyPlugin plugin : plugins) {
            for (String dependency : plugin.descriptor().dependsOn()) {
                if (!byId.containsKey(dependency)) {
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

        return result;
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
            throw new IllegalStateException("Cyclic plugin dependency detected at plugin '" + id + "'");
        }

        visiting.add(id);

        for (String dep : plugin.descriptor().dependsOn()) {
            visit(byId.get(dep), byId, visiting, visited, result);
        }

        visiting.remove(id);
        visited.add(id);
        result.add(plugin);
    }
}
