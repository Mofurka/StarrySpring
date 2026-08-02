package irden.space.proxy.protocol.payload.common.collision;

public enum CollisionKind {
    NULL(0),
    NONE(1),
    PLATFORM(2),
    DYNAMIC(3),
    SLIPPERY(4),
    BLOCK(5);

    private final int id;

    CollisionKind(int id) {
        this.id = id;
    }

    public static CollisionKind fromId(int id) {
        for (CollisionKind kind : values()) {
            if (kind.id == id) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown CollisionKind id: " + id);
    }

    public int id() {
        return id;
    }
}
