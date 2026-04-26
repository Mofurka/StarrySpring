package irden.space.proxy.protocol.payload.packet.warp.player_warp_result;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.WarpActionCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PlayerWarpResultParser implements PacketParser<PlayerWarpResult> {

    @Override
    public PlayerWarpResult parse(BinaryReader reader) {
        boolean warpSuccess = reader.readBoolean();
        WarpAction warpAction = WarpActionCodec.INSTANCE.read(reader);
        boolean warpActionInvalid = reader.readBoolean();
        return new PlayerWarpResult(
                warpSuccess, warpAction, warpActionInvalid
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, PlayerWarpResult payload) {
        writer.writeBoolean(payload.warpSuccess());
        WarpActionCodec.INSTANCE.write(writer, payload.warpAction());
        writer.writeBoolean(payload.warpActionInvalid());
        return finish(writer);
    }
}
