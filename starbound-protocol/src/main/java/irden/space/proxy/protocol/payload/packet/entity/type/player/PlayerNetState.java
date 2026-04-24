package irden.space.proxy.protocol.payload.packet.entity.type.player;

import irden.space.proxy.protocol.payload.common.damage.DamageTeam;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.packet.entity.update.EffectsAnimator;
import lombok.Builder;

import java.util.List;

@Builder
public record PlayerNetState(
        Integer entityId,
        Integer connectionId,
        Integer state, //VLQ
        Boolean shifting,
        Float xMousePos, //accuracy 0.003125
        Float yMousePos,
        HumanoidIdentity humanoidIdentity,
        DamageTeam damageTeam,
        Integer landed,
        String chatMessage,
        Integer newChatMessage,
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
