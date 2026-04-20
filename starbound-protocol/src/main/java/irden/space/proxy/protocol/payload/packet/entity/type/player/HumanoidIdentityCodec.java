package irden.space.proxy.protocol.payload.packet.entity.type.player;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.rgba.Rgba;
import irden.space.proxy.protocol.payload.common.rgba.RgbaCodec;
import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybeCodec;

import java.util.Optional;

public enum HumanoidIdentityCodec implements BinaryCodec<HumanoidIdentity> {
    INSTANCE;
    private final StarMaybeCodec<String> maybeStringCodec = new StarMaybeCodec<>(StarStringCodec.INSTANCE);

    @Override
    public HumanoidIdentity read(BinaryReader reader) {
        String name = StarStringCodec.INSTANCE.read(reader);
        String species = StarStringCodec.INSTANCE.read(reader);
        int gender = reader.readUnsignedByte();
        String hairGroup = StarStringCodec.INSTANCE.read(reader);
        String hairType = StarStringCodec.INSTANCE.read(reader);
        String hairDirectives = StarStringCodec.INSTANCE.read(reader);
        String bodyDirectives = StarStringCodec.INSTANCE.read(reader);
        String emoteDirectives = StarStringCodec.INSTANCE.read(reader);
        String facialHairGroup = StarStringCodec.INSTANCE.read(reader);
        String facialHairType = StarStringCodec.INSTANCE.read(reader);
        String facialHairDirectives = StarStringCodec.INSTANCE.read(reader);
        String facialMaskGroup = StarStringCodec.INSTANCE.read(reader);
        String facialMaskType = StarStringCodec.INSTANCE.read(reader);
        String facialMaskDirectives = StarStringCodec.INSTANCE.read(reader);
        Personality personality = PersonalityCodec.INSTANCE.read(reader);
        Rgba color = RgbaCodec.INSTANCE.read(reader);
        Optional<String> imagePath = maybeStringCodec.read(reader);
        return new HumanoidIdentity(
                name,
                species,
                gender,
                hairGroup,
                hairType,
                hairDirectives,
                bodyDirectives,
                emoteDirectives,
                facialHairGroup,
                facialHairType,
                facialHairDirectives,
                facialMaskGroup,
                facialMaskType,
                facialMaskDirectives,
                personality,
                color,
                imagePath
        );
    }

    @Override
    public void write(BinaryWriter writer, HumanoidIdentity value) {
        StarStringCodec.INSTANCE.write(writer, value.name());
        StarStringCodec.INSTANCE.write(writer, value.species());
        writer.writeByte((byte) value.gender());
        StarStringCodec.INSTANCE.write(writer, value.hairGroup());
        StarStringCodec.INSTANCE.write(writer, value.hairType());
        StarStringCodec.INSTANCE.write(writer, value.hairDirectives());
        StarStringCodec.INSTANCE.write(writer, value.bodyDirectives());
        StarStringCodec.INSTANCE.write(writer, value.emoteDirectives());
        StarStringCodec.INSTANCE.write(writer, value.facialHairGroup());
        StarStringCodec.INSTANCE.write(writer, value.facialHairType());
        StarStringCodec.INSTANCE.write(writer, value.facialHairDirectives());
        StarStringCodec.INSTANCE.write(writer, value.facialMaskGroup());
        StarStringCodec.INSTANCE.write(writer, value.facialMaskType());
        StarStringCodec.INSTANCE.write(writer, value.facialMaskDirectives());
        PersonalityCodec.INSTANCE.write(writer, value.personality());
        RgbaCodec.INSTANCE.write(writer, value.color());
        maybeStringCodec.write(writer, value.imagePath());
    }
}
