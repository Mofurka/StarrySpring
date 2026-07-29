package irden.space.proxy.plugin.general.warp;

import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.model.player_position.CelestialWorld;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerWarpCommands {
    private final PlayerManagerApi playerManagerApi;
    private final EntityMessageService entityMessageService;


    @ChatCommand(value = "warp", aliases = "tp")
    public CommandSpec warpCommand() {
        return CommandSpec.literal("warp")
                .then(
                        CommandSpec.argument("from", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi))
                                .then(
                                        CommandSpec.argument("to", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi)).optional()
                                                .executes(this::handleWarpCommand)
                                )
                )
                .build();
    }

    private void handleWarpCommand(CommandContext ctx) {
        var from = ctx.get("from", PlayerTarget.class).player();
        var to = ctx.getOrNull("to", PlayerTarget.class);

        if (to != null) {
            warp(ctx, from, to.player());
        } else {
            ctx.sender(Player.class).ifPresent(player -> {
                warp(ctx, player, from);
            });

        }


    }

    private void warp(CommandContext ctx, Player from, Player to) {
        if (from.equals(to)) {
            ctx.reply("You can't warp to yourself!");
            return;
        }

        var fromLocation = from.position().getCurrentLocation();
        var toLocation = to.position().getCurrentLocation();

        if (fromLocation instanceof CelestialWorld fromCelestial && toLocation instanceof CelestialWorld toCelestial) {
            if (fromCelestial.equals(toCelestial)) {
                entityMessageService.sendToEntity(from.sessionContext(), from.entityId(), "warp", Variants.of(
                        "Nowhere=" + to.position().getX().intValue() + "." + to.position().getY().intValue()
                ));
                ctx.reply("Warp has been sent!");
                return;
            }
        }

//        ToPlayerWarpAction toPlayerWarpAction = new ToPlayerWarpAction(to.uuid());
//        PlayerWarp playerWarp = new PlayerWarp(toPlayerWarpAction, false);
//        ctx.session().sendToServer(PacketType.PLAYER_WARP, playerWarp);
        entityMessageService.sendToEntity(from.sessionContext(), from.entityId(), "warp", Variants.of(
                "player:" + to.uuid().toString()
        ));
        ctx.reply("Warp has been sent!");

    }

}
