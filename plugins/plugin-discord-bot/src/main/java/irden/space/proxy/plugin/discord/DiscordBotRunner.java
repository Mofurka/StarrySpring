package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public final class DiscordBotRunner implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final DiscordBotFactory botFactory;
    private final CommandHandlerPlugin commandHandler;
    private final RoleManager roleManager;
    private final DiscordBotConfiguration discordBotConfiguration;

    @Getter
    private DiscordBot bot;

    @OnStart
    public void start() {
        Optional.ofNullable(discordBotConfiguration.botToken())
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
