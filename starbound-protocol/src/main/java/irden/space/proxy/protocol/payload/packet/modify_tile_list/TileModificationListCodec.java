package irden.space.proxy.protocol.payload.packet.modify_tile_list;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.star_pair.StarPairCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModification;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.TileModificationCodec;

import java.util.ArrayList;
import java.util.List;

public enum TileModificationListCodec implements BinaryCodec<TileModificationList> {
    INSTANCE;
    private final StarPairCodec<StarVec2I, TileModification> starPairCodec = new StarPairCodec<>(StarVec2ICodec.INSTANCE, TileModificationCodec.INSTANCE);

    @Override
    public TileModificationList read(BinaryReader reader) {
        int size = VlqUCodec.INSTANCE.read(reader);// list size
        List<StarPair<StarVec2I, TileModification>> modifications = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            modifications.add(starPairCodec.read(reader));
        }
        return new TileModificationList(modifications);
    }

    @Override
    public void write(BinaryWriter writer, TileModificationList value) {
        VlqUCodec.INSTANCE.write(writer, value.modifications().size());
        for (StarPair<StarVec2I, TileModification> pair : value.modifications()) {
            starPairCodec.write(writer, pair);
        }
    }
}
