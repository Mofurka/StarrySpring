package irden.space.proxy.protocol.payload.common.string_set;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VlqCodec;

public enum StringSetCodec implements BinaryCodec<StringSet> {
    INSTANCE;

    @Override
    public StringSet read(BinaryReader reader) {
        int size = VlqCodec.INSTANCE.read(reader);
        String[] strings = new String[size];
        for (int i = 0; i < size; i++) {
            strings[i] = StarStringCodec.INSTANCE.read(reader);
        }
        return new StringSet(strings);
    }

    @Override
    public void write(BinaryWriter writer, StringSet value) {
        VlqCodec.INSTANCE.write(writer, value.strings().length);
        for (String string : value.strings()) {
            StarStringCodec.INSTANCE.write(writer, string);
        }
    }
}
