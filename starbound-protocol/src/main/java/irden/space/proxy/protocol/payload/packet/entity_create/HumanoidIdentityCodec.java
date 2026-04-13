package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.maybe.Maybe;
import irden.space.proxy.protocol.payload.common.maybe.MaybeCodec;
import irden.space.proxy.protocol.payload.common.rgba.Rgba;
import irden.space.proxy.protocol.payload.common.rgba.RgbaCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum HumanoidIdentityCodec implements BinaryCodec<HumanoidIdentity> {
    INSTANCE;

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
        Maybe<String> imagePath = new MaybeCodec<>(StarStringCodec.INSTANCE).read(reader);
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
        // TODO: implement
    }
}
