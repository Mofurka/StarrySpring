package irden.space.proxy.protocol.payload.common.interaction;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.interaction.consts.InteractionType;

public enum InteractActionCodec implements BinaryCodec<InteractAction> {
    INSTANCE;

    @Override
    public InteractAction read(BinaryReader reader) {
        InteractionType interactionType = InteractionType.fromId(reader.readInt32BE());
        int targetId = reader.readInt32BE();
        VariantValue interactionData = VariantCodec.INSTANCE.read(reader);
        return new InteractAction(interactionType, targetId, interactionData);
    }

    @Override
    public void write(BinaryWriter writer, InteractAction value) {
        writer.writeInt32BE(value.interactionType().id());
        writer.writeInt32BE(value.targetId());
        VariantCodec.INSTANCE.write(writer, value.interactionData());
    }
}
