package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.server_disconnect.ServerDisconnect;
import irden.space.proxy.protocol.payload.packet.world_stop.WorldStop;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
public class Player {
    private final StarUuid uuid;
    private String name;
    private final String account;
    private final int clientId;
    private final int entityId;
    private final String ipAddress;
    private final String sessionId;
    private final LocalDateTime lastSeen;
    private final PluginSessionContext sessionContext;
    private final Map<Object, Object> metadata = new ConcurrentHashMap<>();

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
    public String account() {
        return account;
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
    public PermissionView permissions() {
        if (!online()) {
            return PermissionView.EMPTY;
        }

        return sessionContext.permissions();
    }
    public LocalDateTime lastSeen() {
        return lastSeen;
    }
    public boolean online() {
        return sessionContext != null;
    }
    public Map<Object, Object> metadata() {
        return metadata;
    }

    public void kick(String reason) {
        if (!online()) {
            return;
        }
        WorldStop worldStop = new WorldStop(reason);
        ServerDisconnect serverDisconnect = new ServerDisconnect(reason);
        sessionContext.sendToClient(PacketType.WORLD_STOP, worldStop);
        sessionContext.sendToClient(PacketType.SERVER_DISCONNECT, serverDisconnect);
    }

    public void sendMessage(ChatReceive message) {
        if (!online()) {
            return;
        }
        sessionContext.sendToClient(PacketType.CHAT_RECEIVE, message);
    }
}
