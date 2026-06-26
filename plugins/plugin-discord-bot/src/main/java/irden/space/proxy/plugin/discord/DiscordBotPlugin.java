package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.command_handler.CommandContextResolver;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    private final CommandHandlerPlugin commandHandler;
    private final CommandContextResolver discordExecutorPlayerResolver;
    private final PluginContext pluginContext;
    private final DiscordBotRunner botRunner;

    public DiscordBotPlugin(
            CommandHandlerPlugin commandHandler,
            CommandContextResolver discordExecutorPlayerResolver,
            PluginContext pluginContext,
            DiscordBotRunner botRunner
    ) {
        this.commandHandler = commandHandler;
        this.discordExecutorPlayerResolver = discordExecutorPlayerResolver;
        this.pluginContext = pluginContext;
        this.botRunner = botRunner;
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
        botRunner.start();
    }
}
