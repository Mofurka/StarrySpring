package irden.space.proxy.protocol.payload.packet.client_disconnect_request;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ClientDisconnectRequestParser implements PacketParser<ClientDisconnectRequest> {
    @Override
    public ClientDisconnectRequest parse(BinaryReader reader, int openProtocolVersion ) {
        boolean b = reader.readBoolean();
        return new ClientDisconnectRequest(b);
    }

    @Override
    public byte[] write(ClientDisconnectRequest payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeBoolean(payload.request());
        return finish(writer);
    }
}
