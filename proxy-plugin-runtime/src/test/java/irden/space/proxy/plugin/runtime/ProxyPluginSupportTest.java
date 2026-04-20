package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProxyPluginSupportTest {

    @Test
    void publishesAnnotatedServicesAndDispatchesSessionLifecycleMethods() {
        RecordingRegistry registry = new RecordingRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotatedPlugin plugin = new AnnotatedPlugin();
        PluginSessionContext sessionContext = new DefaultPluginSessionContext("session-7", "127.0.0.1", false, false);

        plugin.onLoad(pluginContext);
        plugin.onConnectionSuccess(sessionContext);
        plugin.onDisconnecting(sessionContext);
        plugin.onDisconnected(sessionContext);

        GreetingService greetingService = pluginContext.requireService(GreetingService.class);
        GreetingServiceImpl concreteService = pluginContext.requireService(GreetingServiceImpl.class);

        assertEquals("hello", greetingService.greet());
        assertEquals("hello", concreteService.greet());
        assertTrue(greetingService == concreteService);
        assertEquals(
                List.of(
                        "load",
                        "publish-default",
                        "publish-contract",
                        "connected:session-7",
                        "disconnecting:session-7",
                        "disconnected:session-7"
                ),
                plugin.events
        );
    }

    @Test
    void rejectsInvalidPublishServiceSignature() {
        RecordingRegistry registry = new RecordingRegistry();
        PluginContext pluginContext = new DefaultPluginContext(registry);
        InvalidPublishPlugin plugin = new InvalidPublishPlugin();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> plugin.onLoad(pluginContext));
        assertTrue(exception.getMessage().contains("@PublishService"));
    }

    private interface GreetingService {
        String greet();
    }

    private static final class GreetingServiceImpl implements GreetingService {
        @Override
        public String greet() {
            return "hello";
        }
    }

    @SuppressWarnings("unused")
    @PluginDefinition(id = "annotated", name = "Annotated", version = "1.0.0")
    private static final class AnnotatedPlugin implements ProxyPlugin {
        private final List<String> events = new ArrayList<>();

        @OnLoad
        void onLoadHook() {
            events.add("load");
        }

        @PublishService
        GreetingServiceImpl publishDefault() {
            events.add("publish-default");
            return new GreetingServiceImpl();
        }

        @PublishService(GreetingService.class)
        GreetingService publishContract(PluginContext context) {
            events.add("publish-contract");
            return context.requireService(GreetingServiceImpl.class);
        }

        @OnConnectionSuccess
        void onConnectionSuccessHook(PluginSessionContext context) {
            events.add("connected:" + context.sessionId());
        }

        @OnDisconnecting
        void onDisconnectingHook(PluginSessionContext context) {
            events.add("disconnecting:" + context.sessionId());
        }

        @OnDisconnected
        void onDisconnectedHook(PluginSessionContext context) {
            events.add("disconnected:" + context.sessionId());
        }
    }

    @SuppressWarnings("unused")
    @PluginDefinition(id = "invalid-publish", name = "Invalid Publish", version = "1.0.0")
    private static final class InvalidPublishPlugin implements ProxyPlugin {

        @PublishService
        void invalidFactory() {
        }
    }

    private static final class RecordingRegistry implements PacketInterceptorRegistry {
        private final List<PacketInterceptor> interceptors = new ArrayList<>();

        @Override
        public void register(PacketInterceptor interceptor) {
            interceptors.add(interceptor);
        }

        @Override
        public List<PacketInterceptor> getAll() {
            return List.copyOf(interceptors);
        }
    }
}

