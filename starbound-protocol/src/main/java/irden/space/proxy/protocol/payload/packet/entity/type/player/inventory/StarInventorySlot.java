package irden.space.proxy.protocol.payload.packet.entity.type.player.inventory;

public sealed interface StarInventorySlot permits InventoryBagSlot, InventoryEquipmentSlot, InventorySwapSlot, InventoryTrashSlot {
}
