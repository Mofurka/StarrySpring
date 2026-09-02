package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.RegisteredCommand;
import irden.space.proxy.plugin.discord.gateway.DiscordConnection;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DiscordCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DiscordCommandRegistrar.class);

    private final CommandHandlerPlugin commandHandler;
    private final DiscordConnection connection;

    public void registerCommands() {
        Collection<RegisteredCommand> registeredCommands = commandHandler.allCommands();
        List<CommandData> discordCommands = DiscordCommandExporter.export(registeredCommands);

        if (discordCommands.isEmpty()) {
            log.info("No commands to register for Discord bot.");
            return;
        }

        connection.require()
                .updateCommands()
                .addCommands(discordCommands)
                .queue();
    }
}
