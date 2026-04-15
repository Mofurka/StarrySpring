package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record PlayerFirstNetState(
        int state, //VLQ
        boolean shifting,
        StarVec2F mousePos, //accuracy 0.003125
        HumanoidIdentity humanoidIdentity,
        TeamType teamType,
        int teamNumber,
        boolean landed,
        String chatMessage,
        boolean newChatMessage,
        String emote) {


}
