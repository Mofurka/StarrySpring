package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PermissionSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionRegistryApiTest {

    @Test
    void registersAndReadsTypedPermissionFromApi() {
        String permissionName = "test.permission." + UUID.randomUUID();

        Permission registeredPermission = PermissionRegistry.registerIfAbsent(permissionName);
        Permission loadedPermission = PermissionRegistry.getPermission(permissionName);

        assertEquals(registeredPermission, loadedPermission);
        assertEquals(registeredPermission.id(), PermissionRegistry.getPermissionId(permissionName));
        assertTrue(PermissionRegistry.permissionCount() > 0);
        assertTrue(PermissionRegistry.entries().containsKey(permissionName));
    }

    @Test
    void allPermissionIsRegisteredByDefaultAndGrantsAnyPermission() {
        PermissionSet permissionSet = new PermissionSet();
        permissionSet.grantAllAccess();

        Permission permission = PermissionRegistry.registerIfAbsent("test.permission.all." + UUID.randomUUID());

        assertTrue(permissionSet.grantsAll());
        assertTrue(permissionSet.has(PermissionRegistry.ALL));
        assertTrue(permissionSet.has(permission));
        assertTrue(permissionSet.has(permission.id()));
    }

    @Test
    void registerAllReturnsTypedPermissionsThatWorkWithHasAnyAndHasAll() {
        List<Permission> permissions = PermissionRegistry.registerAll(
                "test.permission.batch.read." + UUID.randomUUID(),
                "test.permission.batch.write." + UUID.randomUUID()
        );
        PermissionSet permissionSet = new PermissionSet();

        permissionSet.grantAll(permissions);

        assertTrue(permissionSet.hasAny(permissions));
        assertTrue(permissionSet.hasAll(permissions));

        permissionSet.revokeAll(permissions);

        assertFalse(permissionSet.hasAny(permissions));
    }
}

