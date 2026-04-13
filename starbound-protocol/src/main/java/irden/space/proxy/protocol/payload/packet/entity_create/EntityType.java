package irden.space.proxy.protocol.payload.packet.entity_create;

public enum EntityType {
    PLANT(0),
    OBJECT(1),
    VEHICLE(2),
    ITEM_DROP(3),
    PLANT_DROP(4),
    PROJECTILE(5),
    STAGEHAND(6),
    MONSTER(7),
    NPC(8),
    PLAYER(9);

    private final int id;

    EntityType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static EntityType fromId(int id) {
        for (EntityType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType id: " + id);
    }
}
