package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;
import static org.junit.jupiter.api.Assertions.*;

class StarCustomChatCommandExporterTest {

    @Test
    void shouldExportArgumentChainsAsNestedSubcommandTree() {
        CommandSpec spec = literal("ban")
                .then(argument("target", StringArgumentType.word()).description("Target player")
                        .then(argument("duration", StringArgumentType.word()).optional()
                                .then(argument("reason", StringArgumentType.greedyString()).optional()
                                        .executes(StarCustomChatCommandExporterTest::noop))))
                .build();

        RegisteredCommand command = new RegisteredCommand(
                "test-plugin",
                "ban",
                List.of(),
                "Ban a player from the server.",
                spec
        );

        ListVariantValue exported = StarCustomChatCommandExporter.export(List.of(command));
        MapVariantValue banEntry = map(list(exported).getFirst());

        assertEquals("/ban", string(banEntry, "command"));
        assertEquals("Ban a player from the server.", string(banEntry, "description"));

        List<VariantValue> level1 = list(map(banEntry).get("subcommands"));
        MapVariantValue target = map(level1.getFirst());
        assertEquals("<target>", string(target, "command"));
        assertEquals("Target player", string(target, "description"));

        List<VariantValue> level2 = list(map(target).get("subcommands"));
        MapVariantValue duration = map(level2.getFirst());
        assertEquals("[duration]", string(duration, "command"));

        List<VariantValue> level3 = list(map(duration).get("subcommands"));
        MapVariantValue reason = map(level3.getFirst());
        assertEquals("[reason]", string(reason, "command"));
        assertFalse(map(reason).containsKey("subcommands"));
    }

    @Test
    void shouldExportNestedStaticSubcommandsAndFilterForbiddenBranches() {
        Permission adminPermission = PermissionRegistry.registerIfAbsent("starcustomchat.test.admin");

        CommandSpec spec = literal("user")
                .then(literal("info")
                        .then(argument("identifier", StringArgumentType.word()).description("Player name, UUID or client ID")
                                .executes(StarCustomChatCommandExporterTest::noop)))
                .then(literal("permissions")
                        .permission(adminPermission)
                        .then(argument("identifier", StringArgumentType.word()).description("Permission target")
                                .executes(StarCustomChatCommandExporterTest::noop)))
                .then(literal("role")
                        .then(argument("action", EnumArgumentType.of(TestRoleAction.class))
                                .then(argument("roles", StringArgumentType.word()).description("Role names separated by comma")
                                        .then(argument("identifier", StringArgumentType.word()).description("Role target")
                                                .executes(StarCustomChatCommandExporterTest::noop)))))
                .build();

        RegisteredCommand command = new RegisteredCommand(
                "test-plugin",
                "user",
                List.of(),
                "Manage players.",
                spec
        );

        PermissionSet permissions = new PermissionSet();
        MapVariantValue exported = StarCustomChatCommandExporter.export(command, permissions);
        assertNotNull(exported);

        assertEquals("/user", string(exported, "command"));
        assertEquals("Manage players.", string(exported, "description"));

        List<VariantValue> subcommands = list(map(exported).get("subcommands"));
        assertEquals(2, subcommands.size());

        MapVariantValue info = map(subcommands.getFirst());
        assertEquals("info", string(info, "command"));
        List<VariantValue> infoChildren = list(map(info).get("subcommands"));
        MapVariantValue infoIdentifier = map(infoChildren.getFirst());
        assertEquals("<identifier>", string(infoIdentifier, "command"));
        assertEquals("Player name, UUID or client ID", string(infoIdentifier, "description"));

        MapVariantValue role = map(subcommands.get(1));
        assertEquals("role", string(role, "command"));
        List<VariantValue> roleChildren = list(map(role).get("subcommands"));
        assertEquals(2, roleChildren.size());

        MapVariantValue add = map(roleChildren.getFirst());
        assertEquals("add", string(add, "command"));
        List<VariantValue> addChildren = list(map(add).get("subcommands"));
        MapVariantValue addRoles = map(addChildren.getFirst());
        assertEquals("<roles>", string(addRoles, "command"));
        assertEquals("Role names separated by comma", string(addRoles, "description"));
        List<VariantValue> addRoleChildren = list(map(addRoles).get("subcommands"));
        MapVariantValue roleIdentifier = map(addRoleChildren.getFirst());
        assertEquals("<identifier>", string(roleIdentifier, "command"));
        assertEquals("Role target", string(roleIdentifier, "description"));

        MapVariantValue remove = map(roleChildren.get(1));
        assertEquals("remove", string(remove, "command"));

        assertTrue(subcommands.stream()
                .map(StarCustomChatCommandExporterTest::map)
                .map(entry -> string(entry, "command"))
                .noneMatch("permissions"::equals));
    }

    private static Map<String, VariantValue> map(MapVariantValue value) {
        return value.value();
    }

    private static MapVariantValue map(VariantValue value) {
        return (MapVariantValue) value;
    }

    private static List<VariantValue> list(VariantValue value) {
        return List.of(((ListVariantValue) value).values());
    }

    private static String string(MapVariantValue value, String key) {
        return ((StringVariantValue) map(value).get(key)).value();
    }

    private static void noop(CommandContext ignored) {
    }

    private enum TestRoleAction {
        ADD,
        REMOVE
    }
}

