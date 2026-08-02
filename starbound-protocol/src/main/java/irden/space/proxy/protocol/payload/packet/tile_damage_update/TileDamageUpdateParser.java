package irden.space.proxy.protocol.payload.packet.tile_damage_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamageStatus;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamageStatusCodec;
import irden.space.proxy.protocol.payload.common.tile_layer.TileLayer;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class TileDamageUpdateParser implements PacketParser<TileDamageUpdate> {

    @Override
    public TileDamageUpdate parse(BinaryReader reader) {
        StarVec2I position = StarVec2ICodec.INSTANCE.read(reader);
        TileLayer layer = TileLayer.fromId(reader.readUnsignedByte());
        TileDamageStatus tileDamage = TileDamageStatusCodec.INSTANCE.read(reader);
        return new TileDamageUpdate(position, layer, tileDamage);
    }

    @Override
    public byte[] write(BinaryWriter writer, TileDamageUpdate payload) {
        StarVec2ICodec.INSTANCE.write(writer, payload.position());
        writer.writeByte((byte) payload.layer().id());
        TileDamageStatusCodec.INSTANCE.write(writer, payload.tileDamage());
        return finish(writer);
    }
}
