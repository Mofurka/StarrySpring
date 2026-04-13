package irden.space.proxy.protocol.payload.common.damage.consts;

public enum HitType {
    HIT(0),
    STRONG_HIT(1),
    WEAK_HIT(2),
    SHIELD_HIT(3),
    KILL(4);

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
