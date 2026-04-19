package irden.space.proxy.protocol.payload.common.string_set;

import irden.space.proxy.protocol.codec.*;

public enum StringSetCodec implements BinaryCodec<StringSet> {
    INSTANCE;

    @Override
    public StringSet read(BinaryReader reader) {
        int size = VlqUnsignedCodec.INSTANCE.read(reader);
        String[] strings = new String[size];
        for (int i = 0; i < size; i++) {
            strings[i] = StarStringCodec.INSTANCE.read(reader);
        }
        return new StringSet(strings);
    }

    @Override
    public void write(BinaryWriter writer, StringSet value) {
        VlqUnsignedCodec.INSTANCE.write(writer, value.strings().length);
        for (String string : value.strings()) {
            StarStringCodec.INSTANCE.write(writer, string);
        }
    }
}
