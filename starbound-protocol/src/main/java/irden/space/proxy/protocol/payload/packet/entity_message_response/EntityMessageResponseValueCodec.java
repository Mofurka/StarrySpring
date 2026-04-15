package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;

public enum EntityMessageResponseValueCodec implements BinaryCodec<EntityMessageRsponseValue> {
    INSTANCE;

    @Override
    public EntityMessageRsponseValue read(BinaryReader reader) {
        int success = reader.readUnsignedByte();
        return switch (success) {
            case 1 -> new FailedEntityMessageResponse(
                    StarStringCodec.INSTANCE.read(reader)
            );
            case 2 -> new SuccessEntityMessageResponse(
                    VariantCodec.INSTANCE.read(reader)
            );
            default -> throw new IllegalStateException("Unsupported entity message response value: " + success);
        };
    }

    @Override
    public void write(BinaryWriter writer, EntityMessageRsponseValue value) {
        switch (value) {
            case FailedEntityMessageResponse(String error) -> {
                writer.writeByte(1);
                StarStringCodec.INSTANCE.write(writer, error);
            }
            case SuccessEntityMessageResponse(VariantValue response) -> {
                writer.writeByte(2);
                VariantCodec.INSTANCE.write(writer, response);
            }
            default -> throw new IllegalStateException("Unsupported entity message response value: " + value);
        }

    }
}
