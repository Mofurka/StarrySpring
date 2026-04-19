package irden.space.proxy.protocol.payload.packet.entity.type.player.inventory;

public record InventoryBagSlot(
        String name,
        int slot
) implements StarInventorySlot {
}
