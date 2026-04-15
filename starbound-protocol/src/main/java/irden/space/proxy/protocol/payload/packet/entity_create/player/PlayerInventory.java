package irden.space.proxy.protocol.payload.packet.entity_create.player;

import java.util.Map;

public record PlayerInventory(
        Map<EquipmentSlot, ItemDescriptor> equipment
) {
}
