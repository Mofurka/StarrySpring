package irden.space.proxy.protocol.payload.packet.entity.player;

public enum EquipmentSlot {
    HEAD(0),
    CHEST(1),
    LEGS(2),
    BACK(3),
    HEADCOSMETIC(4),
    CHESTCOSMETIC(5),
    LEGSCOSMETIC(6),
    BACKCOSMETIC(7),
    COSMETIC_1(8),
    COSMETIC_2(9),
    COSMETIC_3(10),
    COSMETIC_4(11),
    COSMETIC_5(12),
    COSMETIC_6(13),
    COSMETIC_7(14),
    COSMETIC_8(15),
    COSMETIC_9(16),
    COSMETIC_10(17),
    COSMETIC_11(18),
    COSMETIC_12(19);

    private final int id;

    public int id() {
        return id;
    }

    EquipmentSlot(int id) {
        this.id = id;
    }

    public static EquipmentSlot fromId(int id) {
        for (EquipmentSlot slot : values()) {
            if (slot.id == id) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Unknown EquipmentSlot id: " + id);
    }
}
