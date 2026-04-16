package irden.space.proxy.protocol.payload.packet.celestial_reqeust;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;
import irden.space.proxy.protocol.payload.common.star_either.StarEither;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3ICodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.ArrayList;

public class CelestialRequestParser implements PacketParser<CelestialRequest> {
    @Override
    public CelestialRequest parse(BinaryReader reader) {
        int size = VlqUCodec.INSTANCE.read(reader); // list size
        var request = new ArrayList<StarEither<StarVec2I, StarVec3I>>();
        for (int i = 0; i < size; i++) {
            if (reader.readBoolean()) {
                StarVec3I vec3I = StarVec3ICodec.INSTANCE.read(reader);
                request.add(StarEither.right(vec3I));
            } else {
                StarVec2I vec2I = StarVec2ICodec.INSTANCE.read(reader);
                request.add(StarEither.left(vec2I));
            }
        }
        return new CelestialRequest(request);
    }

    @Override
    public byte[] write(BinaryWriter writer, CelestialRequest payload) {
        VlqUCodec.INSTANCE.write(writer, payload.celestialRequests().size());
        for (StarEither<StarVec2I, StarVec3I> request : payload.celestialRequests()) {
            if (request.isLeft()) {
                writer.writeBoolean(false);
                StarVec2ICodec.INSTANCE.write(writer, request.left());
            } else {
                writer.writeBoolean(true);
                StarVec3ICodec.INSTANCE.write(writer, request.right());
            }
        }
        return finish(writer);
    }
}
