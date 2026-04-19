package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;

public record DamageTeam(
        TeamType teamType,
        short teamNumber

) {
}
