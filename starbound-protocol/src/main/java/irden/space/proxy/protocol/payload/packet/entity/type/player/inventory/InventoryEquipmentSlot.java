package irden.space.proxy.protocol.payload.packet.entity.type.player.inventory;

import irden.space.proxy.protocol.payload.packet.entity.type.player.EquipmentSlot;

public record InventoryEquipmentSlot(
        EquipmentSlot slot
) implements StarInventorySlot {
}
