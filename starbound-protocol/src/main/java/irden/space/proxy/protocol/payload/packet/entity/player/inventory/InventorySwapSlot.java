package irden.space.proxy.protocol.payload.packet.entity.player.inventory;

public record InventorySwapSlot(
        boolean item
) implements StarInventorySlot {
}
