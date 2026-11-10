package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.ExecutorPlayerContextResolver;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.command.PlayerTargetArgumentType;
import irden.space.proxy.plugin.player_manager.ip.PlayerIpHistoryService;
import irden.space.proxy.plugin.player_manager.ip.PlayerLink;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerIpHistoryEntity;
import irden.space.proxy.plugin.player_manager.roles.RoleActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;

@Component
@RequiredArgsConstructor
public class PlayerCommands {

    private static final DateTimeFormatter SEEN_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PlayerManagerApi playerManagerApi;
    private final PlayerAccessService playerAccessService;
    private final CommandHandlerPlugin commandHandler;
    private final PluginContext pluginContext;
    private final PlayerIpHistoryService ipHistoryService;
    private final PlayerInfoService playerInfoService;

    @OnLoad
    public void registerContextResolver() {
        ExecutorPlayerContextResolver contextResolver = new ExecutorPlayerContextResolver(playerManagerApi);
        commandHandler.addContextResolver(contextResolver);
        pluginContext.onRemove(() -> commandHandler.removeContextResolver(contextResolver));
    }

    @ChatCommand(value = "user",
            aliases = {"u"},
            description = "use for manage players")
    public CommandSpec userCommand() {
        return literal("user").permission(PlayerManagerPermissions.USER.permission())
                .then(literal("info")
                        .then(argument("identifier", PlayerTargetArgumentType.playerTarget(() -> playerManagerApi)).description("Player name, UUID or client ID")
                                .executes(context -> context.reply(
                                        playerInfoService.describe(context.get("identifier", PlayerTarget.class).player())))))
                .then(literal("permissions")
                        .then(argument("identifier", PlayerTargetArgumentType.playerTarget(() -> playerManagerApi)).description("Player name, UUID or client ID")
                                .executes(context -> {
                                    PlayerTarget identifier = context.get("identifier", PlayerTarget.class);
                                    Player player = identifier.player();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Player permissions:").append(System.lineSeparator());
                                    sb.append("- Name: ").append(player.name()).append(System.lineSeparator());
                                    sb.append("- UUID: ").append(player.uuid()).append(System.lineSeparator());

                                    List<String> permissionNames = playerAccessService.listEffectivePermissionNames(player);
                                    if (permissionNames.isEmpty()) {
                                        sb.append("- Effective permissions: none").append(System.lineSeparator());
                                        context.reply(sb.toString());
                                        return;
                                    }

                                    sb.append("- Effective permissions (").append(permissionNames.size()).append("):").append(System.lineSeparator());
                                    if (player.permissions().has(PermissionRegistry.ALL.id())) {
                                        sb.append("  - ").append(PermissionRegistry.ALL.name()).append(System.lineSeparator());
                                    } else {
                                        for (String permissionName : permissionNames) {
                                            sb.append("  - ").append(permissionName).append(System.lineSeparator());
                                        }
                                    }
                                    context.reply(sb.toString());
                                })
                        ))
                .then(literal("links")
                        .then(argument("identifier", PlayerTargetArgumentType.playerTarget(() -> playerManagerApi)).description("Player name, UUID or client ID")
                                .executes(this::handlePlayerLinks)))
                .then(literal("role")
                        .then(argument("action", EnumArgumentType.of(RoleActionType.class))
                                .then(argument("roles", StringArgumentType.word()).description("Role names separated by comma without spaces!")
                                        .then(argument("identifier", PlayerTargetArgumentType.playerTarget(() -> playerManagerApi)).description("Player name, UUID or client ID")
                                                .executes(this::handlePlayerRoleUpdate))))
                )
                .build();
    }

    private void handlePlayerLinks(CommandContext ctx) {
        Player player = ctx.get("identifier", PlayerTarget.class).player();
        String playerUuid = player.uuid().toString();

        List<PlayerIpHistoryEntity> addresses = ipHistoryService.knownAddresses(playerUuid);
        List<PlayerLink> links = ipHistoryService.findLinks(playerUuid);

        StringBuilder sb = new StringBuilder();
        sb.append("Player links:").append(System.lineSeparator());
        sb.append("- Name: ").append(player.name()).append(System.lineSeparator());
        sb.append("- UUID: ").append(playerUuid).append(System.lineSeparator());

        if (addresses.isEmpty()) {
            sb.append("- Known IPs: none yet, history fills up on connect").append(System.lineSeparator());
        } else {
            sb.append("- Known IPs (").append(addresses.size()).append("):").append(System.lineSeparator());
            for (PlayerIpHistoryEntity address : addresses) {
                sb.append("  - ").append(address.getIpAddress())
                        .append(" (last ").append(SEEN_AT.format(address.getLastSeen()))
                        .append(", ").append(address.getConnectionCount()).append(" connections)")
                        .append(System.lineSeparator());
            }
        }

        if (links.isEmpty()) {
            sb.append("- Linked players: none").append(System.lineSeparator());
            ctx.reply(sb.toString());
            return;
        }

        sb.append("- Linked players (").append(links.size()).append("):").append(System.lineSeparator());
        for (PlayerLink link : links) {
            sb.append("  - ").append(link.name() == null ? "<unknown>" : link.name())
                    .append(" [").append(link.playerUuid()).append("]")
                    .append(System.lineSeparator());
            sb.append("    via ").append(String.join(", ", link.sharedAddresses()));
            if (link.lastSeen() != null) {
                sb.append(", last ").append(SEEN_AT.format(link.lastSeen()));
            }
            sb.append(", ").append(link.connectionCount()).append(" connections")
                    .append(System.lineSeparator());
        }

        ctx.reply(sb.toString());
    }

    private void handlePlayerRoleUpdate(CommandContext ctx) {
        PlayerTarget identifier = ctx.get("identifier", PlayerTarget.class);
        Player player = identifier.player();
        RoleActionType actionType = ctx.get("action", RoleActionType.class);
        String[] roleNames = Optional.ofNullable(ctx.get("roles", String.class))
                .map(r -> r.split(",+"))
                .orElse(new String[0]);
        var sb = new StringBuilder();
        for (int i = 0; i < roleNames.length; i++) {
            roleNames[i] = roleNames[i].trim();
            try {
                switch (actionType) {
                    case ADD -> {
                        playerAccessService.assignRoleToPlayer(player.uuid().toString(), roleNames[i], "console");
                        sb.append("Assigned role '%s' to player '%s'%n".formatted(roleNames[i], player.name()));
                    }
                    case REMOVE -> {
                        playerAccessService.removeRoleFromPlayer(player.uuid().toString(), roleNames[i]);
                        sb.append("Removed role '%s' from player '%s'%n".formatted(roleNames
                                [i], player.name()));
                    }
                }
            } catch (Exception e) {
                sb.append("Failed to %s role '%s' for player '%s': %s%n".formatted(
                        actionType == RoleActionType.ADD ? "assign" : "remove",
                        roleNames[i],
                        player.name(),
                        e.getMessage()
                ));
                ctx.reply(sb.toString());
                return;
            }
        }
        ctx.reply(sb.toString());
    }
}
