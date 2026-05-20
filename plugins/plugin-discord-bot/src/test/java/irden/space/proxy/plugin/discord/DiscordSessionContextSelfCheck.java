package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PermissionSet;

public final class DiscordSessionContextSelfCheck {

    private DiscordSessionContextSelfCheck() {
    }

    @SuppressWarnings("unused")
    static void main(String[] args) {
        exposesProvidedPermissions();
        fallsBackToEmptyPermissionsWhenNullWasProvided();
    }

    static void exposesProvidedPermissions() {
        Permission permission = PermissionRegistry.registerIfAbsent("discord.test.permission");
        PermissionSet permissions = new PermissionSet();
        permissions.grant(permission);

        DiscordSessionContext session = new DiscordSessionContext(
                "42",
                "tester",
                "Tester",
                permissions,
                null
        );

        if (session.permissions() != permissions) {
            throw new AssertionError("Expected DiscordSessionContext to expose the provided PermissionView instance");
        }
        if (!session.permissions().has(permission)) {
            throw new AssertionError("Expected DiscordSessionContext to preserve granted permissions");
        }
    }

    static void fallsBackToEmptyPermissionsWhenNullWasProvided() {
        Permission permission = PermissionRegistry.registerIfAbsent("discord.test.empty");

        DiscordSessionContext session = new DiscordSessionContext(
                "42",
                "tester",
                "Tester",
                null,
                null
        );

        if (session.permissions().has(permission)) {
            throw new AssertionError("Expected DiscordSessionContext to fallback to empty permissions when null is provided");
        }
    }
}


