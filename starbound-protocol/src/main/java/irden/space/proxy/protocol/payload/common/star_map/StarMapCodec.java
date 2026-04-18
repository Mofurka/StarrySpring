package irden.space.proxy.protocol.payload.common.star_map;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;

import java.util.LinkedHashMap;
import java.util.Map;

public class StarMapCodec implements BinaryCodec<Map<?, ?>> {
    private final BinaryCodec<?> keyCodec;
    private final BinaryCodec<?> valueCodec;

    public StarMapCodec(BinaryCodec<?> keyCodec, BinaryCodec<?> valueCodec) {
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Override
    public Map<?,?> read(BinaryReader reader) {
        int mapSize = VlqUCodec.INSTANCE.read(reader);
        LinkedHashMap<Object, Object> objectObjectLinkedHashMap = LinkedHashMap.newLinkedHashMap(mapSize);
        for (int i = 0; i < mapSize; i++) {
            Object key = keyCodec.read(reader);
            Object value = valueCodec.read(reader);
            objectObjectLinkedHashMap.put(key, value);
        }
        return objectObjectLinkedHashMap;
    }

    @Override
    public void write(BinaryWriter writer, Map value) {

    }
}
