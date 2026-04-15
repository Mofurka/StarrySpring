package irden.space.proxy.protocol.payload.packet.entity_create.player.inventory;

public record InventoryTrashSlot(
        boolean item
) implements StarInventorySlot {
}
