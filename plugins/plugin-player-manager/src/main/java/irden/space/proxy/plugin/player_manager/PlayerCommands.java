package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.Permission;
import irden.space.proxy.plugin.api.PermissionRegistry;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.ExecutorPlayerContextResolver;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.command.PlayerTargetArgumentType;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.roles.RoleActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;

@Component
@RequiredArgsConstructor
public class PlayerCommands {

    private final PlayerManagerApi playerManagerApi;
    private final PlayerAccessService playerAccessService;
    private final CommandHandlerPlugin commandHandler;
    private final PluginContext pluginContext;

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
                                .executes(context -> {
                                    PlayerTarget identifier = context.get("identifier", PlayerTarget.class);
                                    Player player = identifier.player();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Player info:").append(System.lineSeparator());
                                    sb.append("- Name: ").append(player.name()).append(System.lineSeparator());
                                    sb.append("- UUID: ").append(player.uuid()).append(System.lineSeparator());
                                    sb.append("- Online: ").append(player.online()).append(System.lineSeparator());
                                    if (player.online()) {
                                        sb.append("- Account: ").append(player.account()).append(System.lineSeparator());
                                        sb.append("- Client ID: ").append(player.clientId()).append(System.lineSeparator());
                                        sb.append("- Entity ID: ").append(player.entityId()).append(System.lineSeparator());
                                        sb.append("- IP Address: ").append(player.ipAddress()).append(System.lineSeparator());
                                        Map<String, Object> metadata = player.metadata();
                                        if (!metadata.isEmpty()) {
                                            sb.append("- Metadata: ").append(System.lineSeparator());
                                            metadata.forEach((k, v) -> sb.append("  - ").append(k).append(": ").append(v).append(System.lineSeparator()));
                                        }
                                    }
                                    context.reply(sb.toString());
                                })))
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
                .then(literal("role")
                        .then(argument("action", EnumArgumentType.of(RoleActionType.class))
                                .then(argument("roles", StringArgumentType.word()).description("Role names separated by comma without spaces!")
                                        .then(argument("identifier", PlayerTargetArgumentType.playerTarget(() -> playerManagerApi)).description("Player name, UUID or client ID")
                                                .executes(this::handlePlayerRoleUpdate))))
                )
                .build();
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
