package irden.space.proxy.protocol.payload.packet.damage_notification.consts;

public enum HitType {
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

    HitType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static HitType fromId(int id) {
        for (HitType hitType : values()) {
            if (hitType.id == id) {
                return hitType;
            }
        }
        throw new IllegalArgumentException("Unknown HitType id: " + id);
    }
}
