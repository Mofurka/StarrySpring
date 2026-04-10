package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;

public enum EntityMessageResponseValueCodec implements BinaryCodec<EntityMessageRsponseValue> {
    INSTANCE;

    @Override
    public EntityMessageRsponseValue read(BinaryReader reader) {
        int success = reader.readUnsignedByte();
        if (success == 1) //1 is a failure, 2 is a success
            return new FailedEntityMessageResponse(
                    StarStringCodec.read(reader)
            );
        else if (success == 2)
            return new SuccessEntityMessageResponse(
                    VariantCodec.read(reader)
            );
        else
            throw new IllegalStateException("Unsupported entity message response value: " + success);
    }

    @Override
    public void write(BinaryWriter writer, EntityMessageRsponseValue value) {
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
