package irden.space.proxy.protocol.payload.packet.modify_tile_list;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ModifyTileListCodec implements PacketParser<ModifyTileList> {


    @Override
    public ModifyTileList parse(BinaryReader reader) {
        TileModificationList read = TileModificationListCodec.INSTANCE.read(reader);
        boolean allowEntityOverlap = reader.readBoolean();
        return new ModifyTileList(read, allowEntityOverlap);
    }

    @Override
    public byte[] write(BinaryWriter writer, ModifyTileList payload) {
        TileModificationListCodec.INSTANCE.write(writer, payload.modificationList());
        writer.writeBoolean(payload.allowEntityOverlap());
        return finish(writer);
    }
}
