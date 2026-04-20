package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

import java.time.LocalDateTime;

public class Player {
    private String name;
    private final StarUuid uuid;
    private int clientId;
    private int entityId;
    private final String ipAddress;
    private final String sessionId;
    private final LocalDateTime lastSeen;
    private final PluginSessionContext sessionContext;

    public Player(String name, StarUuid uuid, String ipAddress, String sessionId, PluginSessionContext sessionContext) {
        this.name = name;
        this.uuid = uuid;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.sessionContext = sessionContext;
        this.lastSeen = LocalDateTime.now();
    }

    public String name() {
        return name;
    }
    public void name(String name) {
        this.name = name;
    }
    public StarUuid uuid() {
        return uuid;
    }
    public void clientId(int clientId) {
        this.clientId = clientId;
    }
    public int clientId() {
        return clientId;
    }
    public void entityId(int entityId) {
        this.entityId = entityId;
    }
    public int entityId() {
        return entityId;
    }
    public PluginSessionContext sessionContext() {
        return sessionContext;
    }
    public LocalDateTime lastSeen() {
        return lastSeen;
    }
}
