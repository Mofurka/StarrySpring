package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptorCodec;
import irden.space.proxy.protocol.payload.common.timers.EpochTimer;
import irden.space.proxy.protocol.payload.common.timers.EpochTimerCodec;
import irden.space.proxy.protocol.payload.common.timers.GameTimer;
import irden.space.proxy.protocol.payload.common.timers.GameTimerCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.ItemDropEntity;

public enum ItemDropEntitySpawnCodec implements BinaryCodec<ItemDropEntity> {
    INSTANCE;

    @Override
    public ItemDropEntity read(BinaryReader reader) {
        StarItemDescriptor itemDescriptor = StarItemDescriptorCodec.INSTANCE.read(reader);
        boolean eternal = reader.readBoolean();
        EpochTimer epochTimer = EpochTimerCodec.INSTANCE.read(reader);// despawn timer, we will ignore it for now
        GameTimer intangibleTimer = GameTimerCodec.INSTANCE.read(reader);// despawn timer, we will ignore it for now
        return new ItemDropEntity(itemDescriptor,eternal, epochTimer, intangibleTimer, null);
    }

    @Override
    public void write(BinaryWriter writer, ItemDropEntity value) {
        StarItemDescriptorCodec.INSTANCE.write(writer, value.itemDescriptor());
    }
}
