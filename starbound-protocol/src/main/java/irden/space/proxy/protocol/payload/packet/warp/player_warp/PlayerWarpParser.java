package irden.space.proxy.protocol.payload.packet.warp.player_warp;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.warp.action.WarpAction;
import irden.space.proxy.protocol.payload.common.warp.action.WarpActionCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PlayerWarpParser implements PacketParser<PlayerWarp> {

    @Override
    public PlayerWarp parse(BinaryReader reader, int openProtocolVersion) {
        WarpAction action = WarpActionCodec.read(reader);
        boolean deploy = reader.readBoolean();
        return new PlayerWarp(action, deploy);
    }

    @Override
    public byte[] write(PlayerWarp payload, int openProtocolVersion) {
        BinaryWriter binaryWriter = new BinaryWriter();
        WarpActionCodec.write(binaryWriter, payload.warpAction());
        binaryWriter.writeBoolean(payload.deploy());
        return finish(binaryWriter);
    }
}
