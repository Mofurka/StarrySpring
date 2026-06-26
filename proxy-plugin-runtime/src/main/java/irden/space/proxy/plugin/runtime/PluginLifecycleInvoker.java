package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.annotations.*;

import java.lang.annotation.Annotation;
import java.util.Objects;


public final class PluginLifecycleInvoker {

    private PluginLifecycleInvoker() {
    }

    public static void onLoad(Object bean, PluginContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invoke(bean, OnLoad.class, context);
    }

    public static void onStart(Object bean) {
        Objects.requireNonNull(bean, "bean");
        invoke(bean, OnStart.class);
    }

    public static void onConnectionSuccess(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invoke(bean, OnConnectionSuccess.class, context);
    }

    public static void onDisconnecting(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invoke(bean, OnDisconnecting.class, context);
    }

    public static void onDisconnected(Object bean, PluginSessionContext context) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(context, "context");
        invoke(bean, OnDisconnected.class, context);
    }

    public static void onStop(Object bean) {
        Objects.requireNonNull(bean, "bean");
        invoke(bean, OnStop.class);
    }

    private static void invoke(Object bean, Class<? extends Annotation> annotationType, Object... arguments) {
        var method = PluginAnnotatedMethods.find(bean.getClass(), annotationType);
        if (method == null) {
            return;
        }
        PluginAnnotatedMethods.invoke(bean, method, arguments);
    }
}
