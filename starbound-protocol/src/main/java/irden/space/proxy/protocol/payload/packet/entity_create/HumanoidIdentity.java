package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.payload.common.rgba.Rgba;

import java.util.Optional;

public record HumanoidIdentity(
        String name,
        String species,
        int gender,
        String hairGroup,
        String hairType,
        String hairDirectives,
        String bodyDirectives,
        String emoteDirectives,
        String facialHairGroup,
        String facialHairType,
        String facialHairDirectives,
        String facialMaskGroup,
        String facialMaskType,
        String facialMaskDirectives,
        Personality personality,
        Rgba color,
        Optional<String> imagePath
) {
}
