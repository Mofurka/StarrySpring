package irden.space.proxy.protocol.payload.packet.entity_create.player.inventory;

public record InventorySwapSlot(
        boolean item
) implements StarInventorySlot {
}
