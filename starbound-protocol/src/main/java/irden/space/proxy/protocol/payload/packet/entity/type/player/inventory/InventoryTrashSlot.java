package irden.space.proxy.protocol.payload.packet.entity.type.player.inventory;

public record InventoryTrashSlot(
        boolean item
) implements StarInventorySlot {
}
