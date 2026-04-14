package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum TileLayerCodec implements BinaryCodec<TileLayer> {
    INSTANCE;
    @Override
    public TileLayer read(BinaryReader reader) {
        return TileLayer.fromId(reader.readUnsignedByte());
    }

    @Override
    public void write(BinaryWriter writer, TileLayer value) {
        writer.writeByte((byte) value.id());
    }
}
