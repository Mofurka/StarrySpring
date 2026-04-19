package irden.space.proxy.protocol.payload.packet.entity.type.player.inventory;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.packet.entity.type.player.EquipmentSlot;

public enum StarInventoryCodec implements BinaryCodec<StarInventorySlot> {
    INSTANCE;

    @Override
    public StarInventorySlot read(BinaryReader reader) {
        int variant = VlqUnsignedCodec.INSTANCE.read(reader);
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
                yield new InventorySwapSlot(true);
            }
            case 3 -> {
                yield new InventoryTrashSlot(true);
            }
            default -> throw new IllegalStateException("Unexpected StarInventorySlot value: " + variant);
        };
    }

    @Override
    public void write(BinaryWriter writer, StarInventorySlot value) {

    }
}
