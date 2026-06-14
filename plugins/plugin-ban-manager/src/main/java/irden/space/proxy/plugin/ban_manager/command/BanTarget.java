package irden.space.proxy.plugin.ban_manager.command;

import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.Objects;
import java.util.Optional;

public record BanTarget(String value, Optional<Player> player) {

    public BanTarget {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Ban target must not be blank");
        }
        player = Objects.requireNonNullElse(player, Optional.empty());
    }
}

