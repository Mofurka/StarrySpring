package irden.space.proxy.protocol.payload.common.star_item;

import irden.space.proxy.protocol.codec.variant.VariantValue;

public record StarItemDescriptor (
        String name,
        int count,
        VariantValue parameters
) {
}
