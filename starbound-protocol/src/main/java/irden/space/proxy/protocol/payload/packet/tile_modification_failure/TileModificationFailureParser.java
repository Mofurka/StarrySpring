package irden.space.proxy.protocol.payload.packet.tile_modification_failure;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.TileModificationListCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class TileModificationFailureParser implements PacketParser<TileModificationFailure> {

    @Override
    public TileModificationFailure parse(BinaryReader reader) {
        return new TileModificationFailure(TileModificationListCodec.INSTANCE.read(reader));
    }

    @Override
    public byte[] write(BinaryWriter writer, TileModificationFailure payload) {
        TileModificationListCodec.INSTANCE.write(writer, payload.modificationList());
        return finish(writer);
    }
}
