package irden.space.proxy.plugin.player_manager.model;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import irden.space.proxy.protocol.payload.packet.server_disconnect.ServerDisconnect;
import irden.space.proxy.protocol.payload.packet.world_stop.WorldStop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.intellij.lang.annotations.PrintFormat;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.micrometer.common.util.StringUtils.isBlank;

@Builder
public class Player {
    private final StarUuid uuid;
    private String name;
    private String nickname;
    private String namePrefix;
    private final String account;
    @Builder.Default
    private final int clientId = 0;
    @Builder.Default
    private final int entityId = 0;
    private final String ipAddress;
    private final String sessionId;
    @Builder.Default
    private final LocalDateTime lastSeen = LocalDateTime.now();
    private final PluginSessionContext sessionContext;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

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

    public String nickname() {
        return isBlank(nickname) ? name : nickname;
    }

    public void nickname(String nickname) {
        this.nickname = nickname;
    }

    public String namePrefix() {
        return namePrefix;
    }

    public void namePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
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

    public Map<String, Object> metadata() {
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
        this.sessionContext.close();
    }

    public void sendMessage(@NotNull ChatReceive message) {
        if (!online()) {
            return;
        }
        sessionContext.sendToClient(PacketType.CHAT_RECEIVE, message);
    }

    public void sendMessage(@NotBlank String message) {
        if (!online()) return;

        var header = ChatHeader.builder().mode(ChatReceiveMode.BROADCAST).channel("").clientId(0).build();
        var chatReceive = ChatReceive.builder().header(header).name("Server").message(message).build();

        sessionContext.sendToClient(PacketType.CHAT_RECEIVE, chatReceive);
    }

    public void sendMessage(@PrintFormat String formatMessage, Object... args) {
        sendMessage(String.format(formatMessage, args));
    }
}
