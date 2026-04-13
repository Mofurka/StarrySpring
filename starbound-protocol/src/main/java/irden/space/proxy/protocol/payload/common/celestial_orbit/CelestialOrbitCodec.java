package irden.space.proxy.protocol.payload.common.celestial_orbit;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum CelestialOrbitCodec implements BinaryCodec<CelestialOrbit> {
    INSTANCE;

    @Override
    public CelestialOrbit read(BinaryReader reader) {
        int direction = reader.readInt32BE();
        double enterTime = reader.readDouble64BE();
        StarVec2F enterPosition = StarVec2FCodec.INSTANCE.read(reader);
        return new CelestialOrbit(direction, enterTime, enterPosition);
    }

    @Override
    public void write(BinaryWriter writer, CelestialOrbit value) {
        writer.writeInt32BE(value.direction());
        writer.writeDouble64BE(value.enterTime());
        StarVec2FCodec.INSTANCE.write(writer, value.enterPosition());
    }
}
