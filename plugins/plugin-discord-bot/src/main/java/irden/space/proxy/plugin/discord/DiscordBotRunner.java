package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public final class DiscordBotRunner implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final DiscordBotFactory botFactory;
    private final CommandHandlerPlugin commandHandler;
    private final RoleManager roleManager;

    private DiscordBot bot;

    public DiscordBotRunner(DiscordBotFactory botFactory, CommandHandlerPlugin commandHandler, RoleManager roleManager) {
        this.botFactory = botFactory;
        this.commandHandler = commandHandler;
        this.roleManager = roleManager;
    }

    public void start() {
        Optional.ofNullable(System.getenv("DISCORD_BOT_TOKEN"))
                .ifPresentOrElse(
                        token -> this.bot = botFactory.create(token, commandHandler, roleManager),
                        () -> log.info("DISCORD_BOT_TOKEN is not set; Discord bot will not start")
                );
    }

    @Override
    public void destroy() {
        if (bot != null) {
            log.info("Shutting down Discord bot");
            bot.shutdown();
            bot = null;
        }
    }
}
