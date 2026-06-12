package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.ProxyPlugin;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public final class Pf4jPluginLoader extends PluginLoader {

    private static final Logger log = LoggerFactory.getLogger(Pf4jPluginLoader.class);

    private final PluginLoader classpathPluginLoader;
    private final PluginManager pf4jPluginManager;
    private boolean started;

    public Pf4jPluginLoader(Path pluginsDirectory) {
        this(new PluginLoader(), new DefaultPluginManager(Objects.requireNonNull(pluginsDirectory, "pluginsDirectory")));
    }

    Pf4jPluginLoader(PluginLoader classpathPluginLoader, PluginManager pf4jPluginManager) {
        this.classpathPluginLoader = Objects.requireNonNull(classpathPluginLoader, "classpathPluginLoader");
        this.pf4jPluginManager = Objects.requireNonNull(pf4jPluginManager, "pf4jPluginManager");
    }

    @Override
    public synchronized List<ProxyPlugin> loadPlugins() {
        if (!started) {
            try {
                pf4jPluginManager.loadPlugins();
                pf4jPluginManager.startPlugins();
                started = true;
            } catch (RuntimeException | Error e) {
                pf4jPluginManager.unloadPlugins();
                throw e;
            }
        }

        List<ProxyPlugin> plugins = new ArrayList<>(classpathPluginLoader.loadPlugins());
        List<ProxyPlugin> externalPlugins = pf4jPluginManager.getExtensions(ProxyPlugin.class);
        plugins.addAll(externalPlugins);

        log.info(
                "Discovered {} classpath plugin(s) and {} external PF4J plugin(s)",
                plugins.size() - externalPlugins.size(),
                externalPlugins.size()
        );
        return List.copyOf(plugins);
    }

    @Override
    public synchronized void reloadPlugins(List<ProxyPlugin> plugins) {
        Map<String, Path> pluginPaths = resolveReloadablePluginPaths(plugins);
        List<Map.Entry<String, Path>> pluginsInStopOrder = List.copyOf(pluginPaths.entrySet());
        for (Map.Entry<String, Path> plugin : pluginsInStopOrder) {
            pf4jPluginManager.stopPlugin(plugin.getKey());
            if (!pf4jPluginManager.unloadPlugin(plugin.getKey())) {
                throw new IllegalStateException("Failed to unload PF4J plugin '" + plugin.getKey() + "'");
            }
        }

        List<String> reloadedPluginIds = new ArrayList<>(pluginsInStopOrder.size());
        for (int i = pluginsInStopOrder.size() - 1; i >= 0; i--) {
            String reloadedPluginId = pf4jPluginManager.loadPlugin(pluginsInStopOrder.get(i).getValue());
            reloadedPluginIds.add(reloadedPluginId);
        }
        for (String reloadedPluginId : reloadedPluginIds) {
            pf4jPluginManager.startPlugin(reloadedPluginId);
        }
    }

    @Override
    public synchronized void validateReloadPlugins(List<ProxyPlugin> plugins) {
        resolveReloadablePluginPaths(plugins);
    }

    private Map<String, Path> resolveReloadablePluginPaths(List<ProxyPlugin> plugins) {
        Map<String, Path> pluginPaths = new LinkedHashMap<>();
        for (ProxyPlugin plugin : plugins) {
            PluginWrapper wrapper = pf4jPluginManager.whichPlugin(plugin.getClass());
            if (wrapper == null) {
                continue;
            }

            List<ProxyPlugin> extensions = pf4jPluginManager.getExtensions(ProxyPlugin.class, wrapper.getPluginId());
            if (extensions.size() != 1) {
                throw new IllegalStateException(
                        "PF4J plugin '%s' exports %d ProxyPlugin extensions; runtime reload currently requires exactly one"
                                .formatted(wrapper.getPluginId(), extensions.size())
                );
            }
            pluginPaths.putIfAbsent(wrapper.getPluginId(), wrapper.getPluginPath());
        }
        return pluginPaths;
    }

    @Override
    public synchronized void close() {
        if (!started) {
            return;
        }

        try {
            List<PluginWrapper> startedPlugins = List.copyOf(pf4jPluginManager.getStartedPlugins());
            for (int i = startedPlugins.size() - 1; i >= 0; i--) {
                pf4jPluginManager.stopPlugin(startedPlugins.get(i).getPluginId());
            }
        } finally {
            try {
                pf4jPluginManager.unloadPlugins();
            } finally {
                classpathPluginLoader.close();
                started = false;
            }
        }
    }
}
