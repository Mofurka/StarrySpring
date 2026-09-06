package irden.space.proxy.plugin.irden.d20.initiative.model;

import java.util.Objects;

public record PlayerInFight(
        String name,
        String uuid,
        int initiative,
        FightEntityType entityType
) {

    public PlayerInFight {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(entityType, "entityType");


        if (entityType == FightEntityType.SPECTATOR) {
            initiative = 0;
        }
    }


    public boolean participatesInTurnOrder() {
        return entityType != FightEntityType.SPECTATOR;
    }
}
