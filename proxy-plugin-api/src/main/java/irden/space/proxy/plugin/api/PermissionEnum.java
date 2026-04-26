package irden.space.proxy.plugin.api;

public interface PermissionEnum {

    String permissionNode();

    default Permission permission() {
        return PermissionRegistry.getPermission(permissionNode());
    }

    default void registerDefaults() {
        Class<?> permissionType = getClass();
        if (!permissionType.isEnum()) {
            throw new IllegalStateException("PermissionEnum implementations must be enums: " + permissionType.getName());
        }

        synchronized (permissionType) {
            Object[] enumConstants = permissionType.getEnumConstants();
            if (enumConstants == null) {
                throw new IllegalStateException("Failed to resolve enum constants for permission type " + permissionType.getName());
            }

            for (Object enumConstant : enumConstants) {
                if (!(enumConstant instanceof PermissionEnum permissionEnum)) {
                    throw new IllegalStateException("Enum constant does not implement PermissionEnum: " + permissionType.getName());
                }
                PermissionRegistry.registerIfAbsent(permissionEnum.permissionNode());
            }
        }
    }
}
