package irden.space.proxy.protocol.payload.packet.entity.type;

public sealed interface Entity permits ItemDropEntity, OtherEntity, PlayerEntity, StageHandEntity {
    Integer entityId();
}
