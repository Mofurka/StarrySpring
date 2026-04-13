package irden.space.proxy.protocol.payload.common.damage.consts;

public enum DamageType {
    NO_DAMAGE(0),
    DAMAGE(1),
    IGNORES_DEF(2),
    KNOCKBACK(3),
    ENVIRONMENCT(4),
    STATUS(5);

    private final int id;

    DamageType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static DamageType fromId(int id) {
        for (DamageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DamageType id: " + id);
    }
}
