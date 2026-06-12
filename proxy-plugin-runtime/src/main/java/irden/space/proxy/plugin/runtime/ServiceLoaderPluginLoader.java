package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ServiceLoaderPluginLoader extends PluginLoader {

    @Override
    public List<PluginCandidate> loadPluginCandidates() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceName = "META-INF/services/" + ProxyPlugin.class.getName();
        Set<String> classNames = new LinkedHashSet<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                readProviderClassNames(resources.nextElement(), classNames);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to discover classpath plugin providers", e);
        }

        List<PluginCandidate> candidates = new ArrayList<>();
        for (String className : classNames) {
            Class<? extends ProxyPlugin> pluginClass = loadPluginClass(classLoader, className);
            if (pluginClass.isAnnotationPresent(PluginDefinition.class)) {
                candidates.add(PluginCandidate.fromClass(pluginClass));
            } else {
                throw new IllegalStateException(
                        "Classpath plugin " + className + " must declare @PluginDefinition"
                );
            }
        }
        return List.copyOf(candidates);
    }

    private void readProviderClassNames(URL resource, Set<String> classNames) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                String className = line.split("#", 2)[0].trim();
                if (!className.isEmpty()) {
                    classNames.add(className);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends ProxyPlugin> loadPluginClass(ClassLoader classLoader, String className) {
        try {
            Class<?> candidate = Class.forName(className, false, classLoader);
            if (!ProxyPlugin.class.isAssignableFrom(candidate)) {
                throw new IllegalStateException(className + " does not implement " + ProxyPlugin.class.getName());
            }
            return (Class<? extends ProxyPlugin>) candidate;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load classpath plugin " + className, e);
        }
    }
}
