package irden.space.proxy.protocol.payload.packet.entity_create.player.custom_bar_link;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_maybe.StarMaybeCodec;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.packet.entity_create.player.inventory.StarInventoryCodec;
import irden.space.proxy.protocol.payload.packet.entity_create.player.inventory.StarInventorySlot;

import java.util.Optional;

public enum CustomBarkLinkCodec implements BinaryCodec<CustomBarLink> {
    INSTANCE;
    private final StarMaybeCodec<StarInventorySlot> maybeInventoryCodec = new StarMaybeCodec<>(StarInventoryCodec.INSTANCE);

    @Override
    public CustomBarLink read(BinaryReader reader) {
        StarInventorySlot first = maybeInventoryCodec.read(reader).orElse(null);
        StarInventorySlot second = maybeInventoryCodec.read(reader).orElse(null);
        return new CustomBarLink(new StarPair<>(first, second));


    }

    @Override
    public void write(BinaryWriter writer, CustomBarLink value) {
        maybeInventoryCodec.write(writer, Optional.ofNullable(value.inventorySlot().first()));
        maybeInventoryCodec.write(writer, Optional.ofNullable(value.inventorySlot().second()));

    }

}
