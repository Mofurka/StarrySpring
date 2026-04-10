package irden.space.proxy.protocol.payload.packet.warp.consts;

public enum WarpWorldType {
    CELESTIAL_WORLD(1),
    PLAYER_WORLD(2),
    UNIQUE_WORLD(3),
    MISSION_WORLD(4);

    private final int id;

    WarpWorldType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

        public static WarpWorldType fromId(int id) {
            for (WarpWorldType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown WarpWorldType id: " + id);
        }

}
