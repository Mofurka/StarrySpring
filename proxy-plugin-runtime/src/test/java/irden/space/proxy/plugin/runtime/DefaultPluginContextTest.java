package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPluginContextTest {

    @Test
    void removesOnlyRegistrationsOwnedByPlugin() {
        DefaultPacketInterceptorRegistry interceptorRegistry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext contextManager = new DefaultPluginContext(interceptorRegistry);
        PluginContext firstPlugin = contextManager.forPlugin("first");
        PluginContext secondPlugin = contextManager.forPlugin("second");

        Runnable applicationService = () -> {
        };
        ComparableService firstService = new ComparableService();
        contextManager.publishService(Runnable.class, applicationService);
        firstPlugin.publishService(Comparable.class, firstService);
        firstPlugin.packetInterceptorRegistry().register(PacketType.CHAT_SENT, context -> PacketDecision.forward());
        secondPlugin.packetInterceptorRegistry().register(PacketType.CHAT_RECEIVE, context -> PacketDecision.forward());

        contextManager.removePlugin("first");

        assertSame(applicationService, contextManager.requireService(Runnable.class));
        assertTrue(contextManager.findService(Comparable.class).isEmpty());
        assertEquals(1, interceptorRegistry.getAll().size());
    }

    @Test
    void rejectsServiceReplacementByAnotherOwner() {
        DefaultPluginContext contextManager = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());
        PluginContext firstPlugin = contextManager.forPlugin("first");
        PluginContext secondPlugin = contextManager.forPlugin("second");

        firstPlugin.publishService(Runnable.class, () -> {
        });

        assertThrows(
                IllegalStateException.class,
                () -> secondPlugin.publishService(Runnable.class, () -> {
                })
        );
    }

    @Test
    void runsCleanupCallbacksInReverseOrderAndStillRemovesRegistrationsOnFailure() {
        DefaultPacketInterceptorRegistry interceptorRegistry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext contextManager = new DefaultPluginContext(interceptorRegistry);
        PluginContext plugin = contextManager.forPlugin("plugin");
        StringBuilder cleanupOrder = new StringBuilder();

        plugin.publishService(Comparable.class, new ComparableService());
        plugin.packetInterceptorRegistry().register(PacketType.CHAT_SENT, context -> PacketDecision.forward());
        plugin.onRemove(() -> cleanupOrder.append("first"));
        plugin.onRemove(() -> {
            cleanupOrder.append("second");
            throw new IllegalStateException("cleanup failed");
        });

        assertThrows(IllegalStateException.class, () -> contextManager.removePlugin("plugin"));

        assertEquals("secondfirst", cleanupOrder.toString());
        assertTrue(contextManager.findService(Comparable.class).isEmpty());
        assertTrue(interceptorRegistry.getAll().isEmpty());
    }

    private static final class ComparableService implements Comparable<ComparableService> {
        @Override
        public int compareTo(ComparableService ignored) {
            return 0;
        }
    }
}
