package irden.space.proxy.protocol.payload.packet.entity_create.player.inventory;

import irden.space.proxy.protocol.payload.packet.entity_create.player.EquipmentSlot;

public record InventoryEquipmentSlot(
        EquipmentSlot slot
) implements StarInventorySlot {
}
