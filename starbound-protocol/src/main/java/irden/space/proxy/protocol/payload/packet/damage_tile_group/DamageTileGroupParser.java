package irden.space.proxy.protocol.payload.packet.damage_tile_group;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybeCodec;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamage;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamageCodec;
import irden.space.proxy.protocol.payload.common.tile_layer.TileLayer;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DamageTileGroupParser implements PacketParser<DamageTileGroup> {

    private static final StarMaybeCodec<Integer> SOURCE_ENTITY_CODEC = StarMaybeCodec.of(
            BinaryReader::readInt32BE,
            (writer, entityId) -> writer.writeInt32BE(entityId)
    );

    @Override
    public DamageTileGroup parse(BinaryReader reader) {
        int size = VlqUnsignedCodec.INSTANCE.readInt(reader);
        List<StarVec2I> tilePositions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tilePositions.add(StarVec2ICodec.INSTANCE.read(reader));
        }

        TileLayer layer = TileLayer.fromId(reader.readUnsignedByte());
        StarVec2F sourcePosition = StarVec2FCodec.INSTANCE.read(reader);
        TileDamage tileDamage = TileDamageCodec.INSTANCE.read(reader);
        Optional<Integer> sourceEntity = SOURCE_ENTITY_CODEC.read(reader);

        return new DamageTileGroup(tilePositions, layer, sourcePosition, tileDamage, sourceEntity);
    }

    @Override
    public byte[] write(BinaryWriter writer, DamageTileGroup payload) {
        VlqUnsignedCodec.INSTANCE.write(writer, payload.tilePositions().size());
        for (StarVec2I tilePosition : payload.tilePositions()) {
            StarVec2ICodec.INSTANCE.write(writer, tilePosition);
        }

        writer.writeByte((byte) payload.layer().id());
        StarVec2FCodec.INSTANCE.write(writer, payload.sourcePosition());
        TileDamageCodec.INSTANCE.write(writer, payload.tileDamage());
        SOURCE_ENTITY_CODEC.write(writer, payload.sourceEntity());

        return finish(writer);
    }
}
