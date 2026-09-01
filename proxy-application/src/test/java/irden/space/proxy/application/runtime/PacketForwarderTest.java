package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;
import irden.space.proxy.domain.session.SessionState;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionService;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.PluginSessionLifecycleService;
import irden.space.proxy.protocol.packet.PacketDirection;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PacketForwarderTest {

    @Test
    void closeSessionDispatchesDisconnectLifecycleEventsOnceAndRemovesSession() throws Exception {
        List<String> events = new ArrayList<>();
        ProxySession session = new ProxySession(new ProxySessionId(UUID.randomUUID()), "127.0.0.1");
        session.activate();

        RecordingSessionRegistry sessionRegistry = new RecordingSessionRegistry(events);
        sessionRegistry.add(session);

        RecordingSessionLifecycleService lifecycleService = new RecordingSessionLifecycleService(events);
        PacketInterceptionService packetInterceptionService = ignored -> PacketDecision.forward();

        try (Socket clientSocket = new Socket(); Socket upstreamSocket = new Socket()) {
            ProxySessionRuntimeContext runtimeContext = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new InMemorySessionPermissionService()
            );

            PacketForwarder forwarder = new PacketForwarder(
                    null,
                    null,
                    sessionRegistry,
                    PacketDirection.TO_SERVER,
                    runtimeContext,
                    runtimeContext.clientSideTransport(),
                    null,
                    packetInterceptionService,
                    lifecycleService
            );

            Method closeSession = PacketForwarder.class.getDeclaredMethod("closeSession");
            closeSession.setAccessible(true);
            closeSession.invoke(forwarder);
        }

        assertEquals(SessionState.DISCONNECTED, session.getState());
        assertTrue(sessionRegistry.removed);
        assertEquals(
                List.of(
                        "disconnecting:" + session.getId().uuid(),
                        "removed:" + session.getId().uuid(),
                        "disconnected:" + session.getId().uuid()
                ),
                events
        );
    }


    @Test
    void pluginSessionContextsShareSessionScopedAttributes() throws Exception {
        ProxySession session = new ProxySession(new ProxySessionId(UUID.randomUUID()), "127.0.0.1");
        session.activate();

        try (Socket clientSocket = new Socket(); Socket upstreamSocket = new Socket()) {
            ProxySessionRuntimeContext runtimeContext = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new SwitchableSessionTransport(SessionTransportMode.PLAIN),
                    new InMemorySessionPermissionService()
            );

            PacketForwarder clientToServer = forwarder(runtimeContext, PacketDirection.TO_SERVER);
            PacketForwarder serverToClient = forwarder(runtimeContext, PacketDirection.TO_CLIENT);

            Method createContext = PacketForwarder.class.getDeclaredMethod("createPluginSessionContext", int.class);
            createContext.setAccessible(true);

            PluginSessionContext toServerContext = (PluginSessionContext) createContext.invoke(clientToServer, 5);
            PluginSessionContext toClientContext = (PluginSessionContext) createContext.invoke(serverToClient, 5);

            toServerContext.attributes().put("player", "alice");

            assertEquals("alice", toClientContext.attributes().get("player"));

            PluginSessionContext afterProtocolChange =
                    (PluginSessionContext) createContext.invoke(clientToServer, 7);

            assertNotSame(toServerContext, afterProtocolChange);
            assertEquals("alice", afterProtocolChange.attributes().get("player"));
        }
    }

    private PacketForwarder forwarder(ProxySessionRuntimeContext runtimeContext, PacketDirection direction) {
        return new PacketForwarder(
                null,
                null,
                new RecordingSessionRegistry(new ArrayList<>()),
                direction,
                runtimeContext,
                direction == PacketDirection.TO_SERVER
                        ? runtimeContext.clientSideTransport()
                        : runtimeContext.upstreamSideTransport(),
                null,
                ignored -> PacketDecision.forward(),
                new RecordingSessionLifecycleService(new ArrayList<>())
        );
    }

    private record RecordingSessionLifecycleService(List<String> events) implements PluginSessionLifecycleService {

        @Override
        public void onConnectionSuccess(PluginSessionContext context) {
            events.add("connection-success-unexpected:" + context.sessionId());
        }

        @Override
        public void onDisconnecting(PluginSessionContext context) {
            events.add("disconnecting:" + context.sessionId());
        }

        @Override
        public void onDisconnected(PluginSessionContext context) {
            events.add("disconnected:" + context.sessionId());
        }
    }

    private static final class RecordingSessionRegistry implements SessionRegistry {
        private final List<String> events;
        private ProxySession storedSession;
        private boolean removed;

        private RecordingSessionRegistry(List<String> events) {
            this.events = events;
        }

        @Override
        public void add(ProxySession session) {
            this.storedSession = session;
        }

        @Override
        public void remove(ProxySessionId sessionId) {
            removed = true;
            events.add("removed:" + sessionId.uuid());
            if (storedSession != null && storedSession.getId().equals(sessionId)) {
                storedSession = null;
            }
        }

        @Override
        public Optional<ProxySession> getById(ProxySessionId sessionId) {
            if (storedSession != null && storedSession.getId().equals(sessionId)) {
                return Optional.of(storedSession);
            }
            return Optional.empty();
        }

        @Override
        public Collection<ProxySession> getAllSessions() {
            return storedSession == null ? List.of() : List.of(storedSession);
        }
    }
}

