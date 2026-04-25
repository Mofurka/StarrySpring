package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionView;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPluginSessionContextTest {

    @Test
    void exposesConfiguredPermissionView() {
        Permission kickPermission = new Permission("starry.chat.kick", 42);
        Permission banPermission = new Permission("starry.chat.ban", 7);
        DefaultPluginSessionContext context = new DefaultPluginSessionContext(
                "session-1",
                "127.0.0.1",
                false,
                false,
                1,
                null,
                permissionId -> permissionId == 42
        );

        assertTrue(context.permissions().has(42));
        assertTrue(context.permissions().has(kickPermission));
        assertTrue(context.permissions().hasAny(kickPermission, banPermission));
        assertTrue(context.permissions().hasAll(kickPermission));
        assertFalse(context.permissions().hasAll(kickPermission, banPermission));
        assertFalse(context.permissions().has(7));
    }

    @Test
    void supportsDynamicPermissionLookup() {
        AtomicReference<PermissionView> permissionRef = new AtomicReference<>(PermissionView.EMPTY);
        DefaultPluginSessionContext context = new DefaultPluginSessionContext(
                "session-2",
                "127.0.0.1",
                false,
                false,
                1,
                null,
                permissionId -> permissionRef.get().has(permissionId)
        );

        assertFalse(context.permissions().has(5));

        permissionRef.set(permissionId -> permissionId == 5);

        assertTrue(context.permissions().has(5));
    }
}
