package irden.space.proxy.protocol.payload.packet.modify_tile_list;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModification;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModificationCodec;

import java.util.ArrayList;
import java.util.List;

public enum TileModificationListCodec implements BinaryCodec<TileModificationList> {
    INSTANCE;

    @Override
    public TileModificationList read(BinaryReader reader) {
        int size = VlqCodec.INSTANCE.read(reader);// list size
        List<StarPair<StarVec2I, TileModification>> modifications = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            StarVec2I position = StarVec2ICodec.INSTANCE.read(reader);
            TileModification modification = TileModificationCodec.INSTANCE.read(reader);
            modifications.add(new StarPair<>(position, modification));
        }
        return new TileModificationList(modifications);
    }

    @Override
    public void write(BinaryWriter writer, TileModificationList value) {
        VlqCodec.INSTANCE.write(writer, value.modifications().size());
        for (StarPair<StarVec2I, TileModification> pair : value.modifications()) {
            StarVec2ICodec.INSTANCE.write(writer, pair.first());
            TileModificationCodec.INSTANCE.write(writer, pair.second());
        }
    }
}
