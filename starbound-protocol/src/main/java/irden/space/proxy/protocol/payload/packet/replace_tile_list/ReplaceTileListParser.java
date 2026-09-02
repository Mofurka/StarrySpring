package irden.space.proxy.protocol.payload.packet.replace_tile_list;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamage;
import irden.space.proxy.protocol.payload.common.tile_damage.TileDamageCodec;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.TileModificationList;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.TileModificationListCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ReplaceTileListParser implements PacketParser<ReplaceTileList> {

    private static final int APPLY_DAMAGE_PROTOCOL_VERSION = 7;

    @Override
    public ReplaceTileList parse(BinaryReader reader) {
        TileModificationList modificationList = TileModificationListCodec.INSTANCE.read(reader);
        TileDamage tileDamage = TileDamageCodec.INSTANCE.read(reader);
        boolean applyDamage = false;
        if (reader.openProtocolVersion() >= APPLY_DAMAGE_PROTOCOL_VERSION) {
            applyDamage = reader.readBoolean();
        }
        return new ReplaceTileList(modificationList, tileDamage, applyDamage);
    }

    @Override
    public byte[] write(BinaryWriter writer, ReplaceTileList payload) {
        TileModificationListCodec.INSTANCE.write(writer, payload.modificationList());
        TileDamageCodec.INSTANCE.write(writer, payload.tileDamage());
        if (writer.openProtocolVersion() >= APPLY_DAMAGE_PROTOCOL_VERSION) {
            writer.writeBoolean(payload.applyDamage());
        }
        return finish(writer);
    }
}
