package irden.space.proxy.protocol.payload.common.interaction.consts;

public enum InteractionType {
    NONE(0),
    OPEN_CONTAINTER(1),
    SIT_DOWN(2),
    OPEN_CRAFTING_INTERFACE(3),
    OPEN_SONGBOOK_INTERFACE(4),
    OPEN_NPC_CRAFTING_INTERFACE(5),
    OPEN_MERCHANT_INTERFACE(6),
    OPEN_AI_INTERFACE(7),
    OPEN_TELEPORT_DIALOG(8),
    SHOW_POPUP(9),
    SCRIPT_PANE(10),
    MESSAGE(11);

    private final int id;

    InteractionType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static InteractionType fromId(int id) {
        for (InteractionType hitType : values()) {
            if (hitType.id == id) {
                return hitType;
            }
        }
        throw new IllegalArgumentException("Unknown HitType id: " + id);
    }
}
