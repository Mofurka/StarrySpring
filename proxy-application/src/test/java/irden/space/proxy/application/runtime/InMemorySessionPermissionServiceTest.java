package irden.space.proxy.application.runtime;

import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PermissionSet;
import irden.space.proxy.plugin.api.PermissionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionPermissionServiceTest {

    @Test
    void storesPermissionsPerSessionAndClearsThem() {
        InMemorySessionPermissionService service = new InMemorySessionPermissionService();

        assertFalse(service.permissions("session-a").has(7));

        service.updatePermissions("session-a", permissionId -> permissionId == 7);

        assertTrue(service.permissions("session-a").has(7));
        assertFalse(service.permissions("session-a").has(8));
        assertFalse(service.permissions("session-b").has(7));

        service.updatePermissions("session-a", PermissionView.EMPTY);
        assertFalse(service.permissions("session-a").has(7));

        service.updatePermissions("session-a", permissionId -> permissionId == 11);
        service.clearPermissions("session-a");

        assertFalse(service.permissions("session-a").has(11));
    }

    @Test
    void allPermissionGrantsAnyRegisteredPermissionForSession() {
        InMemorySessionPermissionService service = new InMemorySessionPermissionService();
        PermissionSet permissionSet = new PermissionSet();

        permissionSet.grantAllAccess();
        service.updatePermissions("session-all", permissionSet);

        var dynamicPermission = PermissionRegistry.registerIfAbsent("test.session.all.permission");

        assertTrue(service.permissions("session-all").has(dynamicPermission));
        assertTrue(service.permissions("session-all").has(dynamicPermission.id()));
    }
}
