package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;

public record StarNetItem(
        StarItemDescriptor itemDescriptor,
        StarItemAnimator animator


) {
}
