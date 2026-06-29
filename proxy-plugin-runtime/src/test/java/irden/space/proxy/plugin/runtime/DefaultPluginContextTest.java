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

        firstPlugin.packetInterceptorRegistry().register(PacketType.CHAT_SENT, context -> PacketDecision.forward());
        secondPlugin.packetInterceptorRegistry().register(PacketType.CHAT_RECEIVE, context -> PacketDecision.forward());

        contextManager.removePlugin("first");

        assertEquals(1, interceptorRegistry.getAll().size());
    }

    @Test
    void runsCleanupCallbacksInReverseOrderAndStillRemovesRegistrationsOnFailure() {
        DefaultPacketInterceptorRegistry interceptorRegistry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext contextManager = new DefaultPluginContext(interceptorRegistry);
        PluginContext plugin = contextManager.forPlugin("plugin");
        StringBuilder cleanupOrder = new StringBuilder();

        plugin.packetInterceptorRegistry().register(PacketType.CHAT_SENT, context -> PacketDecision.forward());
        plugin.onRemove(() -> cleanupOrder.append("first"));
        plugin.onRemove(() -> {
            cleanupOrder.append("second");
            throw new IllegalStateException("cleanup failed");
        });

        assertThrows(IllegalStateException.class, () -> contextManager.removePlugin("plugin"));

        assertEquals("secondfirst", cleanupOrder.toString());
        assertTrue(interceptorRegistry.getAll().isEmpty());
    }
}
