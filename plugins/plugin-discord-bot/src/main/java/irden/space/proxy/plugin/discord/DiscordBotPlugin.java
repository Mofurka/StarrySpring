package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.command_handler.CommandContextResolver;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@PluginDefinition(
        id = "discord-bot",
        name = "Discord Bot Plugin",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for discord bot. WIP"
)
public final class DiscordBotPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(DiscordBotPlugin.class);
    private DiscordBot bot;
    private CommandHandlerPlugin commandHandler;
    private CommandContextResolver discordExecutorPlayerResolver;

    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
        this.commandHandler = context.requireService(CommandHandlerPlugin.class);
        this.discordExecutorPlayerResolver = new DiscordExecutorPlayerContextResolver();
        this.commandHandler.addContextResolver(discordExecutorPlayerResolver);
    }

    @OnStart
    public void handleStart() {
        log.info("Starting plugin '{}'", descriptor().id());
        var token = Optional.ofNullable(System.getenv("DISCORD_BOT_TOKEN"));
        token.ifPresent(s -> this.bot = new DiscordBot(s, commandHandler));

    }


    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
        if (bot != null) {
            bot.shutdown();
        }
        if (commandHandler != null && discordExecutorPlayerResolver != null) {
            commandHandler.removeContextResolver(discordExecutorPlayerResolver);
        }
    }


}
