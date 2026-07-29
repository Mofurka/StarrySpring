package irden.space.proxy.plugin.planet_backup;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.native_server_lifespan.ServerLifespan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean({ServerLifespan.class})
public class PlanetBackupCommands {
    private final PlanetBackupCommandHandler commandHandler;


    @ChatCommand(value = "planet-backup", description = "Stop the server, archive universe planets, then restart")
    @SuppressWarnings("unused")
    public CommandSpec backupCommand() {
        return CommandSpec.literal("planet-backup")
                .description("Stop the server, archive universe planets, then restart")
                .executes(context -> {
                    context.reply("Planet backup starting - the server will restart in 30 seconds , expect a brief disconnect.");
                    commandHandler.startDelayedBackup();
                })
                .build();
    }

}
