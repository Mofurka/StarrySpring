package irden.space.proxy.plugin.irden.integration;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.CommandSurface;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.irden.integration.permissions.SitePermissions;
import irden.space.proxy.plugin.player_manager.PlayerAccessService;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteConnectorCommands {
    private final SiteLinker siteLinker;
    private final PlayerAccessService playerAccessService;


    @ChatCommand(value = "link", description = "Links the player to Irden Application Site")
    public CommandSpec link() {
        return CommandSpec.literal("link").surfaces(CommandSurface.IN_GAME)
                .then(CommandSpec.argument("secret", StringArgumentType.greedyString()).executes(
                        ctx -> {
                            String s = ctx.get("secret", String.class);
                            Optional<Player> sender = ctx.sender(Player.class);
                            if (sender.isPresent()) {
                                boolean link = siteLinker.link(sender.get(), s);
                                if (link) {
                                    ctx.reply("Персонаж успешно привязан");
                                    try {
                                        playerAccessService.grantPermissionToPlayer(sender.get().uuid().toString(), SitePermissions.USER_LINKED.permissionNode(), "SITE_LINKER");
                                    } catch (IllegalStateException e) {
                                        log.warn(e.getMessage());
                                    }
                                } else
                                    ctx.reply("Произошла какая-то ошибка. Обратетись к техническому администратору");
                            }
                        }
                )).build();
    }

    @ChatCommand(value = "unlink", description = "Unlink the player from Irden Application Site")
    public CommandSpec unlink() {
        return CommandSpec.literal("unlink")
                .surfaces(CommandSurface.IN_GAME)
                .executes(
                        ctx -> {
                            Optional<Player> sender = ctx.sender(Player.class);
                            if (sender.isPresent()) {
                                boolean unlink = siteLinker.unlink(sender.get());
                                if (unlink) {
                                    ctx.reply("Персонаж успешно отвязан!");
                                    try {
                                        playerAccessService.revokePermissionFromPlayer(sender.get().uuid().toString(), SitePermissions.USER_LINKED.permissionNode(), "SITE_LINKER");
                                    } catch (IllegalStateException e) {
                                        log.warn(e.getMessage());
                                    }
                                    sender.get().kick("Персонаж успешно отвязан. Перезайдите на сервер для применения изменений");
                                } else ctx.reply("Произошла какая-то ошибка. Обратетись к техническому администратору");
                            }
                        }
                ).build();
    }

}
