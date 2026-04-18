package irden.space.proxy.protocol.payload.packet.entity.update;

import lombok.Builder;

import java.util.Map;

@Builder
public record EffectsAnimator(
        String processingDirectives,
        Float zoom,
        Boolean flipped,
        Float flippedRelativeCenterLine,
        Float animationRate,
        Map<String, Object> globalTags
) {
}
