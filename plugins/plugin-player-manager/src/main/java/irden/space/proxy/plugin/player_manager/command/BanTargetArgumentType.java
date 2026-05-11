package irden.space.proxy.plugin.player_manager.command;

import irden.space.proxy.plugin.command_handler.ArgumentParseException;
import irden.space.proxy.plugin.command_handler.ArgumentType;
import irden.space.proxy.plugin.command_handler.CommandArgumentContext;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class BanTargetArgumentType implements ArgumentType<BanTarget> {

    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private final Supplier<PlayerManagerApi> playerManagerApiSupplier;

    private BanTargetArgumentType(Supplier<PlayerManagerApi> playerManagerApiSupplier) {
        this.playerManagerApiSupplier = Objects.requireNonNull(playerManagerApiSupplier, "playerManagerApiSupplier");
    }

    public static BanTargetArgumentType banTarget(PlayerManagerApi playerManagerApi) {
        Objects.requireNonNull(playerManagerApi, "playerManagerApi");
        return new BanTargetArgumentType(() -> playerManagerApi);
    }

    public static BanTargetArgumentType banTarget(Supplier<PlayerManagerApi> playerManagerApiSupplier) {
        return new BanTargetArgumentType(playerManagerApiSupplier);
    }

    @Override
    public BanTarget parse(String input) throws ArgumentParseException {
        return parse(null, input);
    }

    @Override
    public BanTarget parse(CommandArgumentContext context, String input) throws ArgumentParseException {
        if (input == null || input.isBlank()) {
            throw new ArgumentParseException("Ban target must not be blank");
        }

        PlayerManagerApi playerManagerApi = playerManagerApiSupplier.get();
        if (playerManagerApi == null) {
            throw new ArgumentParseException("Player manager is not initialized yet");
        }

        String normalizedInput = input.trim();
        Optional<Player> player = playerManagerApi.findPlayer(normalizedInput, false);

        if (player.isEmpty() && IPV4_PATTERN.matcher(normalizedInput).matches()) {
            player = playerManagerApi.findAllPlayersByIpAddress(normalizedInput).stream().findFirst();
        }

        return new BanTarget(normalizedInput, player);
    }

    @Override
    public String displayName() {
        return "player/ip";
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


