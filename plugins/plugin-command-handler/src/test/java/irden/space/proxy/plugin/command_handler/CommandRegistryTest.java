package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.ProxyPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    @AfterEach
    void clearRegistry() {
        CommandRegistry.global().clear();
    }

    @Test
    void unregistersCommandsAndAliasesOwnedByPlugin() throws Exception {
        TestPlugin firstPlugin = new TestPlugin("first");
        TestPlugin secondPlugin = new TestPlugin("second");
        register(firstPlugin, "firstCommand", "first", "f");
        register(secondPlugin, "secondCommand", "second", "s");

        CommandRegistry.global().unregisterByPluginId("first");

        assertNull(CommandRegistry.global().find("first"));
        assertNull(CommandRegistry.global().find("f"));
        assertNotNull(CommandRegistry.global().find("second"));
        assertNotNull(CommandRegistry.global().find("s"));
        assertEquals(1, CommandRegistry.global().allCommands().size());
    }

    private void register(TestPlugin plugin, String methodName, String name, String alias) throws Exception {
        Method method = TestPlugin.class.getDeclaredMethod(methodName);
        ChatCommand annotation = method.getAnnotation(ChatCommand.class);
        CommandSpec spec = CommandSpec.literal(name)
                .executes(context -> {
                })
                .build();
        CommandRegistry.global().register(plugin, method, annotation, spec);
    }

    private record TestPlugin(String id) implements ProxyPlugin {

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor(id, id, "1.0.0", List.of());
        }

        @ChatCommand(value = "first", aliases = "f")
        public CommandSpec firstCommand() {
            return CommandSpec.literal("first").build();
        }

        @ChatCommand(value = "second", aliases = "s")
        public CommandSpec secondCommand() {
            return CommandSpec.literal("second").build();
        }
    }
}
