package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariant;
import irden.space.proxy.protocol.payload.packet.entity_create.player.custom_bar_link.CustomBarLink;

import java.util.Map;

public record PlayerInventory(
        Map<EquipmentSlot, StarItemDescriptor> equipment,
        Map<Integer, Map<Integer, StarItemDescriptor>> bags,
        StarItemDescriptor cursorItem,
        StarItemDescriptor trashSlot,
        Map<String, Long> currencies,
        int customBarState,
        Map<Integer, Map<Integer, CustomBarLink>> customBar,
        StarMVariant activeSlot,
        StarItemDescriptor beamAxe,
        StarItemDescriptor WireTool,
        StarItemDescriptor paintTool,
        StarItemDescriptor inspectionTool
) {
}
