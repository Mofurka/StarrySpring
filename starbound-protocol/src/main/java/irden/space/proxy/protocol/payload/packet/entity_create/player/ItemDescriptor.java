package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.codec.variant.VariantValue;

import java.util.Optional;

public record ItemDescriptor(
        String name,
        long count,
        VariantValue parameters,
        Optional<Long> parametersHash // i dunno
) {
}
