package irden.space.proxy.protocol.payload.common.interaction;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.interaction.consts.InteractionType;

public record InteractAction(
        InteractionType interactionType,
        int targetId,
        VariantValue interactionData
) {
}
