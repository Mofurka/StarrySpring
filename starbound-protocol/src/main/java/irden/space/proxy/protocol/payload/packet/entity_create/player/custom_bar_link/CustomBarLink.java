package irden.space.proxy.protocol.payload.packet.entity_create.player.custom_bar_link;

import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.packet.entity_create.player.inventory.StarInventorySlot;

public record CustomBarLink(
        StarPair<StarInventorySlot, StarInventorySlot> inventorySlot
) {
}
