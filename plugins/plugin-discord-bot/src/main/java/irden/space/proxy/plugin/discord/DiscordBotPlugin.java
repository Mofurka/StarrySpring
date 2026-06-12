package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.command_handler.CommandContextResolver;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@PluginDefinition(
        id = "discord-bot",
        name = "Discord Bot Plugin",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for discord bot. WIP"
)
@Component
public final class DiscordBotPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(DiscordBotPlugin.class);
    private DiscordBot bot;
    private CommandHandlerPlugin commandHandler;
    private RoleManager roleManager;
    @Autowired
    private DiscordBotFactory botFactory;
    @Autowired
    private CommandContextResolver discordExecutorPlayerResolver;

    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
        this.commandHandler = context.requireService(CommandHandlerPlugin.class);
        this.roleManager = context.requireService(RoleManager.class);
        this.commandHandler.addContextResolver(discordExecutorPlayerResolver);
        context.onRemove(() -> this.commandHandler.removeContextResolver(discordExecutorPlayerResolver));
    }

    @OnStart
    public void handleStart() {
        log.info("Starting plugin '{}'", descriptor().id());
        var token = Optional.ofNullable(System.getenv("DISCORD_BOT_TOKEN"));
        token.ifPresent(s -> this.bot = botFactory.create(s, commandHandler, roleManager));

    }


    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
        if (bot != null) {
            bot.shutdown();
        }
    }


}
