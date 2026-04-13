package irden.space.proxy.protocol.payload.packet.world_start;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

import java.util.List;
import java.util.Map;

public record WorldStart(
        VariantValue templateData,
        byte[] skyData,
        byte[] weatherData,
        StarVec2F playerStart,
        StarVec2F playerRespawn,
        boolean respawnInWorld,
        Map<Short, Float> dungeonIdGravity,
        Map<Short, Boolean> dungeonIdBreathable,
        List<Short> protectedDungeonIds,
        VariantValue worldProperties,
        int connectionId,
        boolean localInterpolationMode
) {
}
