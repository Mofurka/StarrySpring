package irden.space.proxy.plugin.debug.model;

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

    public Player(String name, StarUuid uuid, String ipAddress, String sessionId) {
        this.name = name;
        this.uuid = uuid;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
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
    public LocalDateTime lastSeen() {
        return lastSeen;
    }
}
