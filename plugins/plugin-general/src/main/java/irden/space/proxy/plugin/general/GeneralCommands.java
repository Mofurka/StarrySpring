package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.general.permissions.ChatPermissions;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class GeneralCommands {
    private final GeneralUtils generalUtils;


    @ChatCommand(
            value = "broadcast",
            description = "Broadcast a message to all players."
    )
    @SuppressWarnings("unused")
    public CommandSpec broadcastCommand() {
        return CommandSpec.literal("broadcast").permission(ChatPermissions.BROADCAST.permission())
                .then(CommandSpec.argument("message", StringArgumentType.greedyString())
                        .description("The message to broadcast.")
                        .executes(context -> {
                            String message = context.get("message", String.class);
                            generalUtils.broadcastMessage(message);
                            context.reply("Broadcasted message: " + message);
                        })).build();
    }

    @ChatCommand(value = "who", description = "Shows the list of players to the beggar")
    @SuppressWarnings("unused")
    public CommandSpec whoCommand() {
        return CommandSpec.literal("who").executes(generalUtils::handleWhoCommand).build();
    }


    @ChatCommand(value = "shutdown", description = "Calls a server to perform shutdown action")
    @SuppressWarnings("unused")
    public CommandSpec shutdown() {
        return CommandSpec.literal("shutdown")
                .then(CommandSpec.argument("timer", IntegerArgumentType.integer())
                        .description("Time before the server will shutdown").optional()
                        .executes(ctx -> {
                                    Optional<Player> sender = ctx.sender(Player.class);
                                    final String name;
                                    if (sender.isPresent()) {
                                        name = sender.get().name();
                                    } else name = "Unknown";
                                    var timer = ctx.getOrDefault("timer", Integer.class, 5);
                                    ctx.reply("Server shutdown initiated in %s seconds".formatted(timer));
                                    generalUtils.shutdownServer(name, timer);
                                }
                        )
                )
                .build();
    }


}
