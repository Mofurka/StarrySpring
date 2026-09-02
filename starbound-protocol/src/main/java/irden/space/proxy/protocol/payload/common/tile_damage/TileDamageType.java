package irden.space.proxy.protocol.payload.common.tile_damage;

public enum TileDamageType {
    PROTECTED(0),
    PLANTISH(1),
    BLOCKISH(2),
    BEAMISH(3),
    EXPLOSIVE(4),
    FIRE(5),
    TILLING(6);

    private final int id;

    TileDamageType(int id) {
        this.id = id;
    }

    public static TileDamageType fromId(int id) {
        for (TileDamageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TileDamageType id: " + id);
    }

    public int id() {
        return id;
    }
}
