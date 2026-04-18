package irden.space.proxy.protocol.payload.packet.entity.update;

import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.packet.entity.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity.player.MovementController;
import irden.space.proxy.protocol.payload.packet.entity.player.PlayerInventory;
import lombok.Builder;

import java.util.List;

@Builder
public record PlayerUpdateNetState(
        Integer entityId,
        Integer connectionId,
        Integer state, //VLQ
        Boolean shifting,
        Float xMousePos, //accuracy 0.003125
        Float yMousePos,
        HumanoidIdentity humanoidIdentity,
        TeamType teamType,
        Short teamNumber,
        Boolean landed,
        String chatMessage,
        Boolean newChatMessage,
        String emote,
        PlayerInventory inventory,
        Object tools,
        Object armor,
        Object songbook,
        MovementController movementController,
        List<StarPair<String, String>> effectEmitters,
        EffectsAnimator effectsAnimator
        // Incompleted
)
{
}
