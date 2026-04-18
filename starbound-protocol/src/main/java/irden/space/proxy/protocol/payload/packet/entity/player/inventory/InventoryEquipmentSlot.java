package irden.space.proxy.protocol.payload.packet.entity.player.inventory;

import irden.space.proxy.protocol.payload.packet.entity.player.EquipmentSlot;

public record InventoryEquipmentSlot(
        EquipmentSlot slot
) implements StarInventorySlot {
}
