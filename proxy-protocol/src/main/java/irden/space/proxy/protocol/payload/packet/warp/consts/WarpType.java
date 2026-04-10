package irden.space.proxy.protocol.payload.packet.warp.consts;

public enum WarpType {
    TO_WORLD(1),
    TO_PLAYER(2),
    TO_ALIAS(3);

    private final int id;

    WarpType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static WarpType fromId(int id) {
        for (WarpType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown WarpType id: " + id);
    }
}
