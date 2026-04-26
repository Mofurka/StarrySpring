package irden.space.proxy.plugin.runtime;

import java.util.UUID;

public final class PermissionBootstrapProbe {

    public static final String ON_LOAD_PERMISSION_NODE = "test.permission.bootstrap.onload." + UUID.randomUUID();
    public static final String REGISTRAR_PERMISSION_NODE = "test.permission.bootstrap.registrar." + UUID.randomUUID();
    public static final String REGISTRAR_AWARE_PLUGIN_SIMPLE_NAME = "BootstrapAwarePlugin";

    public static boolean onLoadObservedRegisteredPermission;
    public static boolean registrarObservedRegisteredPermission;

    private PermissionBootstrapProbe() {
    }
}
