package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum PlayerInventoryCodec implements BinaryCodec<PlayerInventory> {
    INSTANCE;


    @Override
    public PlayerInventory read(BinaryReader reader) {
        Map<EquipmentSlot, ItemDescriptor> equipment = new LinkedHashMap<>(EquipmentSlot.values().length);
        for (int i = 0; i < EquipmentSlot.values().length; i++) {
            EquipmentSlot slot = EquipmentSlot.fromId(i);
            String name = StarStringCodec.INSTANCE.read(reader);
            int count = VlqCodec.INSTANCE.read(reader);
            VariantValue parameters = VariantCodec.INSTANCE.read(reader);
             ItemDescriptor itemDescriptor = new ItemDescriptor(name, count, parameters, null);
                equipment.put(slot, itemDescriptor);
        }
        return null;
    }

    @Override
    public void write(BinaryWriter writer, PlayerInventory value) {

    }
}
