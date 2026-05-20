package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public record DiscordSessionContext(
        String userId,
        String userName,
        String displayName,
        PermissionView permissions,
        Object event
) implements PluginSessionContext {

    public DiscordSessionContext {
        permissions = permissions == null ? PermissionView.EMPTY : permissions;
    }

    @Override
    public String sessionId() {
        return "discord:" + userId;
    }

    @Override
    public String clientIp() {
        return "discord";
    }

    @Override
    public boolean clientZstdEnabled() {
        return false;
    }

    @Override
    public boolean upstreamZstdEnabled() {
        return false;
    }

    @Override
    public PermissionView permissions() {
        return permissions;
    }

    @Override
    public void sendToClient(PacketType packetType, Object payload) {
        if (!PacketType.CHAT_RECEIVE.equals(packetType)) {
            return;
        }

        if (!(payload instanceof ChatReceive chatReceive)) {
            throw new IllegalArgumentException("Expected ChatReceive payload but got: " + payload.getClass());
        }

        if (event instanceof SlashCommandInteractionEvent slashEvent) {
            String response = chatReceive.message();
            if (slashEvent.isAcknowledged()) {
                slashEvent.getHook().sendMessage(response).queue();
            } else {
                slashEvent.reply(response).queue();
            }
        }
    }
}

