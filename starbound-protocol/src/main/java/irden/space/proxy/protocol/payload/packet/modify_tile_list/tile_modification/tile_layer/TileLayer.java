package irden.space.proxy.protocol.payload.packet.modify_tile_list.tile_modification.tile_layer;

public enum TileLayer {
    FOREGROUND(0),
    BACKGROUND(1);

    private final int id;

    TileLayer(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static TileLayer fromId(int id) {
        for (TileLayer layer : values()) {
            if (layer.id == id) {
                return layer;
            }
        }
        throw new IllegalArgumentException("Invalid TileLayer id: " + id);
    }
}
