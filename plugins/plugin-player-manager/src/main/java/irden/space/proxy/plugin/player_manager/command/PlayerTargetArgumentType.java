package irden.space.proxy.plugin.player_manager.command;

import irden.space.proxy.plugin.command_handler.ArgumentParseException;
import irden.space.proxy.plugin.command_handler.ArgumentType;
import irden.space.proxy.plugin.command_handler.CommandArgumentContext;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class PlayerTargetArgumentType implements ArgumentType<PlayerTarget> {

    private final Supplier<PlayerManagerApi> playerManagerApiSupplier;

    private PlayerTargetArgumentType(Supplier<PlayerManagerApi> playerManagerApiSupplier) {
        this.playerManagerApiSupplier = Objects.requireNonNull(playerManagerApiSupplier, "playerManagerApiSupplier");
    }

    public static PlayerTargetArgumentType playerTarget(PlayerManagerApi playerManagerApi) {
        Objects.requireNonNull(playerManagerApi, "playerManagerApi");
        return new PlayerTargetArgumentType(() -> playerManagerApi);
    }

    public static PlayerTargetArgumentType playerTarget(Supplier<PlayerManagerApi> playerManagerApiSupplier) {
        return new PlayerTargetArgumentType(playerManagerApiSupplier);
    }

    @Override
    public PlayerTarget parse(String input) throws ArgumentParseException {
        return parse(null, input);
    }

    @Override
    public PlayerTarget parse(CommandArgumentContext context, String input) throws ArgumentParseException {
        if (input == null || input.isBlank()) {
            throw new ArgumentParseException("Player target must not be blank");
        }

        PlayerManagerApi playerManagerApi = playerManagerApiSupplier.get();
        if (playerManagerApi == null) {
            throw new ArgumentParseException("Player manager is not initialized yet");
        }

        String normalizedInput = input.trim();
        Player player = playerManagerApi.findPlayer(normalizedInput, false)
                .orElseThrow(() -> new ArgumentParseException("Player not found: " + normalizedInput));

        return new PlayerTarget(normalizedInput, player);
    }

    @Override
    public String displayName() {
        return "player";
    }

    @Override
    public boolean supportsAutocomplete() {
        return true;
    }

    @Override
    public List<String> suggestions(CommandContext context, String prefix) {
        PlayerManagerApi playerManagerApi = playerManagerApiSupplier.get();
        if (playerManagerApi == null) {
            return List.of();
        }

        return playerManagerApi.searchPlayers(prefix, 25, false).stream()
                .map(Player::name)
                .distinct()
                .toList();
    }
}
