package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class Player {
    private final StarUuid uuid;
    private String name;
    private final int clientId;
    private final int entityId;
    private final String ipAddress;
    private final String sessionId;
    private final LocalDateTime lastSeen;
    private final PluginSessionContext sessionContext;

    public String name() {
        return name;
    }
    public void name(String name) {
        this.name = name;
    }
    public StarUuid uuid() {
        return uuid;
    }
    public int clientId() {
        return clientId;
    }
    public int entityId() {
        return entityId;
    }
    public String ipAddress() {
        return ipAddress;
    }
    public String sessionId() {
        return sessionId;
    }
    public PluginSessionContext sessionContext() {
        return sessionContext;
    }
    public LocalDateTime lastSeen() {
        return lastSeen;
    }
}
