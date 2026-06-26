package irden.space.boot;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.*;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandRegistry;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.runtime.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginSpringBeanAnnotationRegistrationTest {

    @Test
    void registersPacketHandlersAndCommandsFromPluginSpringBeans() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        PluginManager manager = new PluginManager(
                new PluginLoader() {
                    @Override
                    public List<PluginCandidate> loadPluginCandidates() {
                        return List.of(PluginCandidate.fromClass(BeanAnnotationPlugin.class));
                    }
                },
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                new SpringPluginContainerFactory(rootContext)
        );

        manager.loadAndStart();

        assertEquals(1, registry.getAll().size());
        PacketDecision decision = registry.getAll().getFirst().intercept(packetContext(PacketType.CHAT_SENT));
        assertSame(PacketDecision.cancel(), decision);
        assertNotNull(CommandRegistry.global().find("bean-command"));

        manager.stopAll();
        rootContext.close();

        assertEquals(0, registry.getAll().size());
        assertNull(CommandRegistry.global().find("bean-command"));
    }

    @Test
    void invokesLifecycleCallbacksAndPermissionsFromPluginSpringBeans() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        DefaultPluginContext pluginContext = new DefaultPluginContext(registry);
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        List<String> events = new ArrayList<>();
        rootContext.registerBean("lifecycleEvents", List.class, () -> events);
        rootContext.refresh();
        PluginManager manager = new PluginManager(
                new PluginLoader() {
                    @Override
                    public List<PluginCandidate> loadPluginCandidates() {
                        return List.of(PluginCandidate.fromClass(BeanLifecyclePlugin.class));
                    }
                },
                new PluginDependencyResolver(),
                registry,
                pluginContext,
                new SpringPluginContainerFactory(rootContext)
        );

        manager.loadAndStart();
        manager.onConnectionSuccess(sessionContext("session-1"));
        manager.onDisconnecting(sessionContext("session-1"));
        manager.onDisconnected(sessionContext("session-1"));
        manager.stopAll();
        rootContext.close();

        assertEquals(
                List.of(
                        "permissions",
                        "permission-visible-on-load:true",
                        "start",
                        "connected:session-1",
                        "disconnecting:session-1",
                        "disconnected:session-1",
                        "stop"
                ),
                events
        );
    }

    private PacketInterceptionContext packetContext(PacketType packetType) {
        return new PacketInterceptionContext(
                null,
                new PacketEnvelope(
                        packetType.id(),
                        packetType,
                        0,
                        false,
                        new byte[0],
                        new byte[0],
                        PacketDirection.TO_SERVER
                ),
                null,
                PacketDirection.TO_SERVER
        );
    }

    private PluginSessionContext sessionContext(String sessionId) {
        return new DefaultPluginSessionContext(sessionId, "127.0.0.1", false, false);
    }

    @PluginDefinition(id = "spring-bean-annotations", name = "Spring Bean Annotations", version = "1.0.0")
    @PluginSpringConfiguration(value = BeanAnnotationConfiguration.class, scanPluginPackage = false)
    static final class BeanAnnotationPlugin implements ProxyPlugin {
    }

    static final class BeanAnnotationConfiguration {
        @Bean
        PacketHandlerBean packetHandlerBean() {
            return new PacketHandlerBean();
        }

        @Bean
        CommandBean commandBean() {
            return new CommandBean();
        }
    }

    @PluginDefinition(id = "spring-bean-lifecycle", name = "Spring Bean Lifecycle", version = "1.0.0")
    @PluginSpringConfiguration(value = BeanLifecycleConfiguration.class, scanPluginPackage = false)
    static final class BeanLifecyclePlugin implements ProxyPlugin {
    }

    static final class BeanLifecycleConfiguration {
        @Bean
        LifecycleBean lifecycleBean(List<String> lifecycleEvents) {
            return new LifecycleBean(lifecycleEvents);
        }
    }

    static final class PacketHandlerBean {
        @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
        public PacketDecision onChatSent(PacketInterceptionContext context) {
            return PacketDecision.cancel();
        }
    }

    static final class CommandBean {
        @ChatCommand("bean-command")
        public CommandSpec beanCommand() {
            return CommandSpec.literal("bean-command")
                    .executes(context -> {
                    })
                    .build();
        }
    }

    static final class LifecycleBean {
        private final List<String> events;

        LifecycleBean(List<String> events) {
            this.events = events;
        }

        @RegisterPluginPermissions
        public Class<? extends PermissionEnum> permissions() {
            events.add("permissions");
            return TestPermissions.class;
        }

        @OnLoad
        public void onLoad() {
            events.add("permission-visible-on-load:" + PermissionRegistry.contains("test.lifecycle"));
        }

        @OnStart
        public void onStart() {
            events.add("start");
        }

        @OnConnectionSuccess
        public void onConnectionSuccess(PluginSessionContext context) {
            events.add("connected:" + context.sessionId());
        }

        @OnDisconnecting
        public void onDisconnecting(PluginSessionContext context) {
            events.add("disconnecting:" + context.sessionId());
        }

        @OnDisconnected
        public void onDisconnected(PluginSessionContext context) {
            events.add("disconnected:" + context.sessionId());
        }

        @OnStop
        public void onStop() {
            events.add("stop");
        }
    }

    enum TestPermissions implements PermissionEnum {
        LIFECYCLE("test.lifecycle");

        private final String permissionNode;

        TestPermissions(String permissionNode) {
            this.permissionNode = permissionNode;
        }

        @Override
        public String permissionNode() {
            return permissionNode;
        }
    }
}
