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
import irden.space.proxy.plugin.player_manager.model.player_position.InstanceWorld;
import irden.space.proxy.plugin.player_manager.model.player_position.PlayerShip;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.warp.action.ToWorldWarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;
import irden.space.proxy.protocol.payload.common.warp.target.CelestialWorldTarget;
import irden.space.proxy.protocol.payload.common.warp.target.PlayerWorldTarget;
import irden.space.proxy.protocol.payload.common.warp.target.UniqueWorldTarget;
import irden.space.proxy.protocol.payload.common.warp.target.WorldTarget;
import irden.space.proxy.protocol.payload.packet.warp.consts.SpawnTarget;
import irden.space.proxy.protocol.payload.packet.warp.player_warp.PlayerWarp;
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
            ctx.sender(Player.class).ifPresent(player -> warp(ctx, player, from));

        }


    }

    private void warp(CommandContext ctx, Player from, Player to) {
        if (from.equals(to)) {
            ctx.reply("You can't warp to yourself!");
            return;
        }

        var fromLocation = from.position().getCurrentLocation();
        var toLocation = to.position().getCurrentLocation();

        if (fromLocation instanceof CelestialWorld fromCelestial && toLocation instanceof CelestialWorld toCelestial && fromCelestial.equals(toCelestial)) {
            entityMessageService.sendToEntity(from.sessionContext(), from.entityId(), "warp", Variants.of(
                    "Nowhere=" + to.position().getX().intValue() + "." + to.position().getY().intValue()
            ));
            ctx.reply("Warp has been sent!");
            return;
        }

        WorldTarget worldTarget = switch (toLocation) {
            case CelestialWorld(var location, var planet, var satellite) -> {
                var cc = new CelestialCoordinates(location, planet, satellite);
                var ue = new SpawnTarget.Position(new StarVec2F(to.position().getX(), to.position().getY()));
                yield new CelestialWorldTarget(cc, ue);
            }
            case InstanceWorld(var worldName, var instanceUuid) ->
                    new UniqueWorldTarget(worldName, instanceUuid, null, null);
            case PlayerShip(var shipUuid) ->
                    new PlayerWorldTarget(shipUuid, new SpawnTarget.Position(new StarVec2F(to.position().getX(), to.position().getY())));
        };

        WarpAction warpAction = new ToWorldWarpAction(worldTarget);
        PlayerWarp playerWarp = new PlayerWarp(warpAction, false);
        from.sessionContext().sendToServer(PacketType.PLAYER_WARP, playerWarp);
        ctx.reply("Warp has been sent!");
    }

}
