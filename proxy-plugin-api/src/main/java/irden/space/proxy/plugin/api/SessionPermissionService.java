package irden.space.proxy.plugin.api;

public interface SessionPermissionService {

    PermissionView permissions(String sessionId);

    void updatePermissions(String sessionId, PermissionView permissions);

    void clearPermissions(String sessionId);
}
