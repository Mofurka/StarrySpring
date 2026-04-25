package irden.space.proxy.application.runtime;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.SessionPermissionService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemorySessionPermissionService implements SessionPermissionService {

    private final Map<String, PermissionView> permissionsBySessionId = new ConcurrentHashMap<>();

    @Override
    public PermissionView permissions(String sessionId) {
        if (sessionId == null) {
            return PermissionView.EMPTY;
        }

        return permissionsBySessionId.getOrDefault(sessionId, PermissionView.EMPTY);
    }

    @Override
    public void updatePermissions(String sessionId, PermissionView permissions) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session id must not be null");
        }

        permissionsBySessionId.put(sessionId, permissions == null ? PermissionView.EMPTY : permissions);
    }

    @Override
    public void clearPermissions(String sessionId) {
        if (sessionId == null) {
            return;
        }

        permissionsBySessionId.remove(sessionId);
    }
}
