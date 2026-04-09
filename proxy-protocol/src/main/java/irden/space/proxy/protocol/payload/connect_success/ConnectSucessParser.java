package irden.space.proxy.protocol.payload.connect_success;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.PacketParser;

public class ConnectSucessParser implements PacketParser<ConnectSuccess> {

    @Override
    public ConnectSuccess parse(BinaryReader reader) {
        int clientId = VlqCodec.read(reader);
        String uuid = bytesTohex(reader.readBytes(16));
        return new ConnectSuccess(
                clientId,
                uuid,
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE()
        );
    }

    @Override
    public byte[] write(ConnectSuccess payload) {
        BinaryWriter writer = new BinaryWriter();
        VlqCodec.write(writer, payload.clientId());
        writer.writeBytes(hexToBytes(payload.uuid()));
        writer.writeInt32BE(payload.planetOrbitalLevels());
        writer.writeInt32BE(payload.satelliteOrbitalLevels());
        writer.writeInt32BE(payload.chunkSize());
        writer.writeInt32BE(payload.xyMin());
        writer.writeInt32BE(payload.xyMax());
        writer.writeInt32BE(payload.zMin());
        writer.writeInt32BE(payload.zMax());
        return finish(writer);
    }

    private static String bytesTohex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return result;
    }
}
