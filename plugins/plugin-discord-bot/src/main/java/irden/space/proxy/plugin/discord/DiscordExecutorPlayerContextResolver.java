package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandContextResolver;
import irden.space.proxy.plugin.player_manager.command.PlayerManagerCommandContextKeys;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public final class DiscordExecutorPlayerContextResolver implements CommandContextResolver {

    @Override
    public void resolve(CommandContext.Builder builder) {
        if (!(builder.session() instanceof DiscordSessionContext session)) {
            return;
        }

        Player executorPlayer = Player.builder()
                .uuid(StarUuid.fromJavaUuid(UUID.nameUUIDFromBytes(session.sessionId().getBytes(StandardCharsets.UTF_8))))
                .name(String.format("<@!%s>", session.userId()))
                .account(session.userName())
                .ipAddress(session.clientIp())
                .sessionId(session.sessionId())
                .lastSeen(LocalDateTime.now())
                .sessionContext(session)
                .build();

        builder.put(PlayerManagerCommandContextKeys.EXECUTOR_PLAYER, executorPlayer);
    }
}

