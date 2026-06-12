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
    private final CommandHandlerPlugin commandHandler;
    private final RoleManager roleManager;
    private final DiscordBotFactory botFactory;
    private final CommandContextResolver discordExecutorPlayerResolver;
    private final PluginContext pluginContext;

    public DiscordBotPlugin(
            CommandHandlerPlugin commandHandler,
            RoleManager roleManager,
            DiscordBotFactory botFactory,
            CommandContextResolver discordExecutorPlayerResolver,
            PluginContext pluginContext
    ) {
        this.commandHandler = commandHandler;
        this.roleManager = roleManager;
        this.botFactory = botFactory;
        this.discordExecutorPlayerResolver = discordExecutorPlayerResolver;
        this.pluginContext = pluginContext;
    }

    @OnLoad
    public void handleLoad() {
        log.info("Loading plugin '{}'", descriptor().id());
        this.commandHandler.addContextResolver(discordExecutorPlayerResolver);
        pluginContext.onRemove(() -> this.commandHandler.removeContextResolver(discordExecutorPlayerResolver));
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
