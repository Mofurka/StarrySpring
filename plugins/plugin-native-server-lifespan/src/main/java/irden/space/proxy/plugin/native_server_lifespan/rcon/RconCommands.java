package irden.space.proxy.plugin.native_server_lifespan.rcon;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RconCommands {
    private final StarboundRconClient starboundRconClient;


    @ChatCommand("rcon")
    public CommandSpec rconCommand() {
        return CommandSpec.literal("rcon")
                .description("rcon command")
                .then(
                        CommandSpec.argument("command", StringArgumentType.greedyString())
                                .executes(
                                        context -> {
                                            var command = context.get("command", String.class);
                                            starboundRconClient.executeUnchecked(command).ifPresentOrElse(
                                                    context::reply,
                                                    () -> context.reply("Done")
                                            );
                                        }
                                )
                ).build();
    }

}
