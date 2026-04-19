package irden.space.proxy.protocol.payload.common.damage.consts;

public enum     TeamType {
    NULL(1),
    FRIENDLY(2),
    ENEMY(3),
    PVP(4),
    PASSIVE(5),
    GHOSTLY(6),
    ENVIRONMENT(7),
    INDISCRIMINATE(8),
    ASSISTANT(9);

    private final int id;

    TeamType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static TeamType fromId(int id) {
        for (TeamType hitType : values()) {
            if (hitType.id == id) {
                return hitType;
            }
        }
        throw new IllegalArgumentException("Unknown HitType id: " + id);
    }
}
