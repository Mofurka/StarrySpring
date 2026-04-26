package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.PermissionEnum;

enum PermissionBootstrapTestPermissions implements PermissionEnum {
    ON_LOAD(PermissionBootstrapProbe.ON_LOAD_PERMISSION_NODE),
    REGISTRAR(PermissionBootstrapProbe.REGISTRAR_PERMISSION_NODE);

    private final String permissionNode;

    PermissionBootstrapTestPermissions(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    @Override
    public String permissionNode() {
        return permissionNode;
    }
}
