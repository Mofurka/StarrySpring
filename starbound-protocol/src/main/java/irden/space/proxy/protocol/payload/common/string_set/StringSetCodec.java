package irden.space.proxy.protocol.payload.common.string_set;

import irden.space.proxy.protocol.codec.*;

public enum StringSetCodec implements BinaryCodec<StringSet> {
    INSTANCE;

    @Override
    public StringSet read(BinaryReader reader) {
        int size = VlqCodec.read(reader);
        String[] strings = new String[size];
        for (int i = 0; i < size; i++) {
            strings[i] = StarStringCodec.read(reader);
        }
        return new StringSet(strings);
    }

    @Override
    public void write(BinaryWriter writer, StringSet value) {
        VlqCodec.write(writer, value.strings().length);
        for (String string : value.strings()) {
            StarStringCodec.write(writer, string);
        }
    }
}
