package irden.space.proxy.protocol.payload.packet.damage;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.damage.DamageRequest;
import irden.space.proxy.protocol.payload.common.damage.DamageRequestCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class RemoteDamageRequestParser implements PacketParser<RemoteDamageRequest> {
    @Override
    public RemoteDamageRequest parse(BinaryReader reader, int openProtocolVersion) {
        int sourceId = reader.readInt32BE();
        int targetId = reader.readInt32BE();
        DamageRequest damageRequest = DamageRequestCodec.INSTANCE.read(reader);
        return new RemoteDamageRequest(sourceId, targetId, damageRequest);
    }

    @Override
    public byte[] write(RemoteDamageRequest payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeInt32BE(payload.sourceId());
        writer.writeInt32BE(payload.targetId());
        DamageRequestCodec.INSTANCE.write(writer, payload.damageRequest());
        return finish(writer);
    }
}
