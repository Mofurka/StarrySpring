package irden.space.boot;

import irden.space.proxy.plugin.api.PluginAnnotationRegistrar;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ServiceLoader;

final class PluginNativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ClassLoader effectiveClassLoader = classLoader != null ? classLoader : PluginNativeRuntimeHints.class.getClassLoader();

        registerServiceProviders(hints, effectiveClassLoader, ProxyPlugin.class);
        registerServiceProviders(hints, effectiveClassLoader, PluginAnnotationRegistrar.class);
    }

    private <T> void registerServiceProviders(RuntimeHints hints, ClassLoader classLoader, Class<T> serviceType) {
        hints.resources().registerPattern("META-INF/services/" + serviceType.getName());

        ServiceLoader.load(serviceType, classLoader)
                .stream()
                .map(ServiceLoader.Provider::type)
                .forEach(providerType -> registerProviderReflection(hints, providerType));
    }

    private void registerProviderReflection(RuntimeHints hints, Class<?> providerType) {
        for (Constructor<?> constructor : providerType.getDeclaredConstructors()) {
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
        }

        for (Method method : providerType.getDeclaredMethods()) {
            hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
        }
    }
}

