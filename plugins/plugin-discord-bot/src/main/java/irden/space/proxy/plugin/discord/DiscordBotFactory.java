package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.discord.model.DiscordRoleManager;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class DiscordBotFactory {

    private final ObjectProvider<DiscordRoleManager> discordRoleManagerProvider;

    public DiscordBotFactory(ObjectProvider<DiscordRoleManager> discordRoleManagerProvider) {
        this.discordRoleManagerProvider = discordRoleManagerProvider;
    }

    public DiscordBot create(String token, CommandHandlerPlugin commandHandler, RoleManager roleManager) {
        return new DiscordBot(token, commandHandler, roleManager, discordRoleManagerProvider.getObject());
    }
}
