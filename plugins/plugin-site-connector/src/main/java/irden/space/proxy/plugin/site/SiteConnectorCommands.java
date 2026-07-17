package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SiteConnectorCommands {
    private final SiteLinker siteLinker;


    @ChatCommand(value = "link", description = "Links the player to Irden Application Site")
    public CommandSpec link() {
        return CommandSpec.literal("link").then(CommandSpec.argument("secret", StringArgumentType.greedyString()).executes(
                ctx -> {
                    String s = ctx.get("secret", String.class);
                    Optional<Player> sender = ctx.sender(Player.class);
                    if (sender.isPresent()) {
                        boolean link = siteLinker.link(sender.get(), s);
                        if (link)
                            ctx.reply("Персонаж успешно привязан");
                        else
                            ctx.reply("Произошла какая-то ошибка. Обратетись к техническому администратору");
                    }
                }
        )).build();
    }

    @ChatCommand(value = "unlink", description = "Unlink the player from Irden Application Site")
    public CommandSpec unlink() {
        return CommandSpec.literal("unlink").executes(
                ctx -> {
                    Optional<Player> sender = ctx.sender(Player.class);
                    if (sender.isPresent()) {
                        boolean unlink = siteLinker.unlink(sender.get());
                        if (unlink) ctx.reply("Персонаж успешно отвязан!");
                        else ctx.reply("Произошла какая-то ошибка. Обратетись к техническому администратору");
                    }
                }
        ).build();
    }

}
