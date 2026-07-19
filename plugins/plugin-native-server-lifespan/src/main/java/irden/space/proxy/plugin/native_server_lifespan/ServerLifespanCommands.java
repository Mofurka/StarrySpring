package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.EnumArgumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(
        value = "native-server-lifespan.enabled",
        matchIfMissing = true
)
public class ServerLifespanCommands {
    private final ServerLifespanCommandHandler handler;

    @ChatCommand(
            "native-server"
    )
    public CommandSpec handleServerCommand() {
        return CommandSpec.literal("native-server").then(
                CommandSpec.argument("argument", EnumArgumentType.of(NativeServerCommandEnumArgument.class)).executes(handler::handleCommand)
        ).build();
    }

    public enum NativeServerCommandEnumArgument {
        START, STOP, RESTART, INFO
    }

}
