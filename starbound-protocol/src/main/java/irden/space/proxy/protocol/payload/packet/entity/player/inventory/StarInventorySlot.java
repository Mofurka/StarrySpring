package irden.space.proxy.protocol.payload.packet.entity.player.inventory;

public sealed interface StarInventorySlot permits InventoryBagSlot, InventoryEquipmentSlot, InventorySwapSlot, InventoryTrashSlot {
}
