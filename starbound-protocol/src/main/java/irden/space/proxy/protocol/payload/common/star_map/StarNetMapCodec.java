package irden.space.proxy.protocol.payload.common.star_map;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;

import java.util.LinkedHashMap;
import java.util.Map;

public class StarNetMapCodec<A,B> implements BinaryCodec<Map<A, B>> {
    private final BinaryCodec<A> keyCodec;
    private final BinaryCodec<B> valueCodec;

    public StarNetMapCodec(BinaryCodec<A> keyCodec, BinaryCodec<B> valueCodec) {
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }


    @Override
    public Map<A, B> read(BinaryReader reader) {
        int size = VlqUCodec.INSTANCE.read(reader); // VLQ, не byte!
        Map<A, B> map = LinkedHashMap.newLinkedHashMap(size);
        for (int i = 0; i < size; i++) {
            int changeCode = reader.readUnsignedByte(); // 0=Set, 1=Remove, 2=Clear
            if (changeCode == 0) { // SetChange
                A key = keyCodec.read(reader);
                B value = valueCodec.read(reader);
                map.put(key, value);
            } else if (changeCode == 1) { // RemoveChange
                Object read = keyCodec.read(reader);// skip, не добавляем, но прочитать нужно
                map.remove(read);
            } else if (changeCode == 2) { // ClearChange
                map.clear();
            }

        }
        return map;
    }

    public Map<A, B> readDelta(BinaryReader reader) {
        Map<A, B> map = new LinkedHashMap<>();

        while (true) {
            int code = VlqUCodec.INSTANCE.read(reader);  // ← VLQ!

            if (code == 0) {
                break;
            }
            else if (code == 1) {  // Full reload
                map = this.read(reader);
            }
            else if (code == 2) {  // Single change
                int changeCode = reader.readUnsignedByte();  // ← byte!

                if (changeCode == 0) {
                    A key = keyCodec.read(reader);
                    B value = valueCodec.read(reader);
                    map.put(key, value);
                }
                else if (changeCode == 1) {
                    A key = keyCodec.read(reader);
                    map.remove(key);
                }
                else if (changeCode == 2) {
                    map.clear();
                }
            }
            else {
                throw new IllegalStateException("Unknown NetElementHashMap delta code: " + code);
            }
        }

        return map;
    }

    @Override
    public void write(BinaryWriter writer, Map<A, B> value) {
        VlqUCodec.INSTANCE.write(writer, value.size());
        for (Map.Entry<A, B> entry : value.entrySet()) {
            writer.writeByte(0); // SetChange
            keyCodec.write(writer, entry.getKey());
            valueCodec.write(writer, entry.getValue());
        }
    }


}
