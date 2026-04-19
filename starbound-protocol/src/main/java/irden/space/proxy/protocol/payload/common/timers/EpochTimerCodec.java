package irden.space.proxy.protocol.payload.common.timers;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.base_types.DoubleCodec;
import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybeCodec;

import java.util.Optional;

public enum EpochTimerCodec implements BinaryCodec<EpochTimer> {
    INSTANCE;
    private final StarMaybeCodec<Double> maybeDoubleCodec = new StarMaybeCodec<>(DoubleCodec.INSTANCE);


    @Override
    public EpochTimer read(BinaryReader reader) {
        Optional<Double> lastSeenEpochTime = maybeDoubleCodec.read(reader);
        double elapsedTime = reader.readDouble64BE();
        return new EpochTimer(lastSeenEpochTime, elapsedTime);
    }

    @Override
    public void write(BinaryWriter writer, EpochTimer value) {
        maybeDoubleCodec.write(writer, value.lastSeenEpochTime());
        writer.writeDouble64BE(value.elapsedTime());
    }
}
