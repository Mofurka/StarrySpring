package irden.space.proxy.plugin.player_manager.command;

import irden.space.proxy.plugin.command_handler.CommandContextKey;
import irden.space.proxy.plugin.player_manager.model.Player;

public final class PlayerManagerCommandContextKeys {

    public static final CommandContextKey<Player> EXECUTOR_PLAYER =
            CommandContextKey.of("executorPlayer", Player.class);

    private PlayerManagerCommandContextKeys() {
    }
}

