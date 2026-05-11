package irden.space.proxy.plugin.player_manager.command;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandContextResolver;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;

import java.util.Objects;

public final class ExecutorPlayerContextResolver implements CommandContextResolver {

    private final PlayerManagerApi playerManagerApi;

    public ExecutorPlayerContextResolver(PlayerManagerApi playerManagerApi) {
        this.playerManagerApi = Objects.requireNonNull(playerManagerApi, "playerManagerApi");
    }

    @Override
    public void resolve(CommandContext.Builder builder) {
        playerManagerApi.getPlayerBySessionId(builder.session().sessionId())
                .ifPresent(player -> builder.put(PlayerManagerCommandContextKeys.EXECUTOR_PLAYER, player));
    }
}

