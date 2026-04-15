package irden.space.proxy.protocol.payload.packet.entity_create.player.inventory;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.packet.entity_create.player.EquipmentSlot;

public enum StarInventoryCodec implements BinaryCodec<StarInventorySlot> {
    INSTANCE;

    @Override
    public StarInventorySlot read(BinaryReader reader) {
        int variant = VlqCodec.INSTANCE.read(reader);
        return switch (variant) {
            case 0 -> {
                var equipmentSlot = EquipmentSlot.fromId(reader.readUnsignedByte());
                yield  new InventoryEquipmentSlot(equipmentSlot);
            }
            case 1 -> {
                String bagName = StarStringCodec.INSTANCE.read(reader);// bag name
                int bagIndex = reader.readUnsignedByte();// bag index
                yield new InventoryBagSlot(bagName, bagIndex);
            }
            case 2 -> {
                boolean b = reader.readBoolean();// swap
                yield new InventorySwapSlot(!b);
            }
            case 3 -> {
                boolean b = reader.readBoolean();// swap
                yield new InventoryTrashSlot(!b);
            }
            default -> throw new IllegalStateException("Unexpected StarInventorySlot value: " + variant);
        };
    }

    @Override
    public void write(BinaryWriter writer, StarInventorySlot value) {

    }
}
