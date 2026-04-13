package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class EntityMessageResponseValueCodec {

    public static EntityMessageRsponseValue read(BinaryReader reader) {
        int success = reader.readUnsignedByte();
        return switch (success) {
            case 1 -> new FailedEntityMessageResponse(
                    StarStringCodec.read(reader)
            );
            case 2 -> new SuccessEntityMessageResponse(
                    VariantCodec.read(reader)
            );
            default -> throw new IllegalStateException("Unsupported entity message response value: " + success);
        };
    }

    public static void write(BinaryWriter writer, EntityMessageRsponseValue value) {
        switch (value) {
            case FailedEntityMessageResponse(String error) -> {
                writer.writeByte(1);
                StarStringCodec.write(writer, error);
            }
            case SuccessEntityMessageResponse(VariantValue response) -> {
                writer.writeByte(2);
                VariantCodec.write(writer, response);
            }
            default -> throw new IllegalStateException("Unsupported entity message response value: " + value);
        }

    }
}
