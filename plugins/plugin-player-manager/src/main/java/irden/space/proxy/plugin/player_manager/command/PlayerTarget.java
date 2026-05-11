package irden.space.proxy.plugin.player_manager.command;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.Objects;

public record PlayerTarget(String value, Player player) {

    public PlayerTarget {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(player, "player");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Player target must not be blank");
        }
    }
}
