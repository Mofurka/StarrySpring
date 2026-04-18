package irden.space.proxy.protocol.payload.common.ephemeral_status_effect;

import irden.space.proxy.protocol.codec.*;

import java.util.ArrayList;
import java.util.List;

public enum EphemeralStatusEffectCodec implements BinaryCodec<EphemeralStatusEffect> {
    INSTANCE;

    @Override
    public EphemeralStatusEffect read(BinaryReader reader) {
        String name = StarStringCodec.INSTANCE.read(reader);
        int type = reader.readUnsignedByte();

        if (type == 0) {
            return new StringStatusEffect(name);
        } else if (type == 1) {
            float duration = reader.readFloat32BE();
            return new JsonStatusEffect(name, duration);
        } else {
            throw new IllegalStateException("Unknown ephemeral status effect type: " + type);
        }
    }

    @Override
    public void write(BinaryWriter writer, EphemeralStatusEffect value) {
        switch (value) {
            case StringStatusEffect(String name) -> {
                StarStringCodec.INSTANCE.write(writer, name);
                writer.writeByte(0);
            }
            case JsonStatusEffect(String name, float duration) -> {
                StarStringCodec.INSTANCE.write(writer, name);
                writer.writeByte(1);
                writer.writeFloat32BE(duration);
            }
        }
    }

    public List<EphemeralStatusEffect> readList(BinaryReader reader) {
        int size = VlqUCodec.INSTANCE.read(reader);
        List<EphemeralStatusEffect> effects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            effects.add(read(reader));
        }
        return effects;
    }

    public void writeList(BinaryWriter writer, List<EphemeralStatusEffect> effects) {
        VlqUCodec.INSTANCE.write(writer, effects.size());
        for (EphemeralStatusEffect effect : effects) {
            write(writer, effect);
        }
    }
}
