package irden.space.proxy.plugin.irden.ingame.doors.model;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

import java.util.List;

public record IrdenDoorPaidEntryOffer(
        String doorUuid,
        Long doorId,
        Integer sourceId,
        String account,
        Long price,
        List<Integer> destination,
        String warpTarget
) {
}
