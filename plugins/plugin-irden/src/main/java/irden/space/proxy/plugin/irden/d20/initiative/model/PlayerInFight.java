package irden.space.proxy.plugin.irden.d20.initiative.model;


public record PlayerInFight(
        String name,
        String uuid,
        int initiative,
        FightEntityType entityType
) {
}

