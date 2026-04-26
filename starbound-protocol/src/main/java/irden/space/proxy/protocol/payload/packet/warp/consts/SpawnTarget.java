package irden.space.proxy.protocol.payload.packet.warp.consts;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public sealed interface SpawnTarget {
    record UniqueEntity(String entityName) implements SpawnTarget {}
    record Position(StarVec2F position) implements SpawnTarget {}  // ВОТ ОНИ - КООРДИНАТЫ!
    record XCoordinate(float x) implements SpawnTarget {}
}
