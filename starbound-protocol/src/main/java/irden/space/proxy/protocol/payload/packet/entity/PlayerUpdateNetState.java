package irden.space.proxy.protocol.payload.packet.entity;

import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;
import irden.space.proxy.protocol.payload.packet.entity.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity.player.MovementController;
import irden.space.proxy.protocol.payload.packet.entity.player.PlayerInventory;
import lombok.Builder;

@Builder
public record PlayerUpdateNetState(
        int state, //VLQ
        boolean shifting,
        Float xMousePos, //accuracy 0.003125
        Float yMousePos,
        HumanoidIdentity humanoidIdentity,
        TeamType teamType,
        int teamNumber,
        boolean landed,
        String chatMessage,
        boolean newChatMessage,
        String emote,
        PlayerInventory inventory,
        Object tools,
        Object armor,
        Object songbook,
        MovementController movementController
        // Incompleted
)
{
}
