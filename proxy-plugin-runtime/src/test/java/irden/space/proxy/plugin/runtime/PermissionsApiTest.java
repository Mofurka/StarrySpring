package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.DefaultPluginSessionContext;
import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.Permissions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionsApiTest {

    @Test
    void createsPermissionSetsWithHelpers() {
        Permission read = new Permission("test.read", 10);
        Permission write = new Permission("test.write", 11);

        PermissionSet granted = Permissions.granted(read, write);
        PermissionSet none = Permissions.none();
        PermissionSet allAccess = Permissions.allAccess();

        assertTrue(granted.hasAll(read, write));
        assertFalse(none.hasAny(read, write));
        assertTrue(allAccess.grantsAll());
    }

    @Test
    void checksPermissionsThroughPluginSessionContextHelpers() {
        Permission kick = new Permission("test.kick", 42);
        Permission ban = new Permission("test.ban", 43);
        DefaultPluginSessionContext context = new DefaultPluginSessionContext(
                "session-helper",
                "127.0.0.1",
                false,
                false,
                1,
                null,
                permissionId -> permissionId == 42
        );

        assertTrue(Permissions.has(context, kick));
        assertTrue(Permissions.hasAny(context, kick, ban));
        assertFalse(Permissions.hasAll(context, kick, ban));
        assertFalse(Permissions.view(null).has(kick));
        assertTrue(Permissions.view(context).has(kick));
        assertFalse(PermissionView.EMPTY.has(kick));
    }
}

