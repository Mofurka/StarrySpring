package irden.space.proxy.protocol.payload.common.string_set;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VlqCodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class StringSetCodec {

    public static StringSet read(BinaryReader reader) {
        int size = VlqCodec.read(reader);
        String[] strings = new String[size];
        for (int i = 0; i < size; i++) {
            strings[i] = StarStringCodec.read(reader);
        }
        return new StringSet(strings);
    }

    public static void write(BinaryWriter writer, StringSet value) {
        VlqCodec.write(writer, value.strings().length);
        for (String string : value.strings()) {
            StarStringCodec.write(writer, string);
        }
    }
}
