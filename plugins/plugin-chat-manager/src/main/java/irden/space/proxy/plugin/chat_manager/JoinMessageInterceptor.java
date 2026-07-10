package irden.space.proxy.plugin.chat_manager;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.micrometer.common.util.StringUtils.isBlank;

@Component
@RequiredArgsConstructor
public class JoinMessageInterceptor {
    private static final Pattern JOIN_PATTERN =
            Pattern.compile("^Player '([^']+)' (dis)?connected$");
    private final ChatManagerPlugin plugin;
    private final MessageSource  messageSource;

    @PacketHandler(
            value = PacketType.CHAT_RECEIVE,
            direction = PacketDirection.TO_CLIENT
    )
    @SuppressWarnings("unused")
    public PacketDecision onPlayerJoin(PacketInterceptionContext ctx) {
        ChatReceive chatReceive = ctx.parsedPayload(ChatReceive.class);

        if (chatReceive == null || chatReceive.message() == null) {
            return PacketDecision.forward();
        }

        Matcher matcher = JOIN_PATTERN.matcher(chatReceive.message());

        if (!matcher.matches()) {
            return PacketDecision.forward();
        }
        return PacketDecision.cancel();
    }

    @EventListener
    public void onPlayerJoin(PlayerConnectedEvent event) {
        Player player = event.player();
        final String playerName = isBlank(player.nickname()) ? player.name() : player.nickname();
        final String finalPlayerName = Color.colorString(player.namePrefix(), playerName, true);
        String joinMsg = messageSource.getMessage("chat.player.join", new Object[]{finalPlayerName}, Locale.getDefault());
        plugin.broadcastMessage(joinMsg);
    }

}