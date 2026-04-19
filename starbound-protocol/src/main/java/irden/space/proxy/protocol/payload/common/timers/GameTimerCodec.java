package irden.space.proxy.protocol.payload.common.timers;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum GameTimerCodec implements BinaryCodec<GameTimer> {
    INSTANCE;

    @Override
    public GameTimer read(BinaryReader reader) {
        float time = reader.readFloat32BE();
        float timer = reader.readFloat32BE();
        return new GameTimer(time, timer);
    }

    @Override
    public void write(BinaryWriter writer, GameTimer value) {
        writer.writeFloat32BE(value.timer());
        writer.writeFloat32BE(value.time());
    }
}
