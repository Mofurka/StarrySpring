package irden.space.proxy.protocol.payload.common.star_pair;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public class StarPairCodec<A,B> implements BinaryCodec<StarPair<A,B>>{
    private final BinaryCodec<A> firstCodec;
    private final BinaryCodec<B> secondCodec;

    public StarPairCodec(BinaryCodec<A> firstCodec, BinaryCodec<B> secondCodec) {
        this.firstCodec = firstCodec;
        this.secondCodec = secondCodec;
    }


    @Override
    public StarPair<A, B> read(BinaryReader reader) {
        A first = firstCodec.read(reader);
        B second = secondCodec.read(reader);
        return new StarPair<>(first, second);
    }

    @Override
    public void write(BinaryWriter writer, StarPair<A, B> value) {
        firstCodec.write(writer, value.first());
        secondCodec.write(writer, value.second());
    }
}
