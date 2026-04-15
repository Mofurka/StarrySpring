package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptorCodec;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariant;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariantCodec;
import irden.space.proxy.protocol.payload.common.star_map.StarNetMapCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.packet.entity_create.player.custom_bar_link.CustomBarLink;
import irden.space.proxy.protocol.payload.packet.entity_create.player.custom_bar_link.CustomBarkLinkCodec;

import java.util.*;

public enum PlayerInventoryCodec implements BinaryCodec<PlayerInventory> {
    INSTANCE;
    private final StarMVariantCodec starMVariantCodec = new StarMVariantCodec(
            VlqCodec.INSTANCE,
            VlqCodec.INSTANCE
    );
    private final StarNetMapCodec<String, String> starNetMapCodec = new StarNetMapCodec<>(
            StarStringCodec.INSTANCE,
            StarStringCodec.INSTANCE
    );


    @Override
    public PlayerInventory read(BinaryReader reader) {
        final int protocolVersion = reader.openProtocolVersion();
        int mapSize = protocolVersion >= 9 ? 20 : 8;
        Map<EquipmentSlot, StarItemDescriptor> equipment = LinkedHashMap.newLinkedHashMap(mapSize);

        for (int i = 0; i < mapSize; i++) {
            EquipmentSlot slot = EquipmentSlot.fromId(i);
            StarItemDescriptor item = StarItemDescriptorCodec.INSTANCE.read(reader);// item
            equipment.put(slot, item);
        }
        final int bagsCount = 5;
        final int bagCapacity = 40;
        Map<Integer, Map<Integer, StarItemDescriptor>> bags = LinkedHashMap.newLinkedHashMap(bagsCount);
        for (int i = 0; i < bagsCount; i++) {
            for (int j = 0; j < bagCapacity; j++) {
                StarItemDescriptor item = StarItemDescriptorCodec.INSTANCE.read(reader);
                bags.computeIfAbsent(i, _ -> LinkedHashMap.newLinkedHashMap(bagCapacity)).put(j, item);
            }
        }
        StarItemDescriptor cursorItem = StarItemDescriptorCodec.INSTANCE.read(reader);
        StarItemDescriptor trashSlot = StarItemDescriptorCodec.INSTANCE.read(reader);

        int currenciesMapSize = VlqCodec.INSTANCE.read(reader); // currencies
        Map<String, Long> stringMap = LinkedHashMap.newLinkedHashMap(currenciesMapSize);
        for (int i = 0; i < currenciesMapSize; i++) {
            String key = StarStringCodec.INSTANCE.read(reader);
            long ammount = reader.readInt64BE();
            stringMap.put(key, ammount);
        }

        int customBarState = VlqCodec.INSTANCE.read(reader); // state
        final int customBarIndexes = 6;
        final int customBarSize = 2;
        Map<Integer, Map<Integer, CustomBarLink>> customBar = LinkedHashMap.newLinkedHashMap(customBarIndexes);
        for (int i = 0; i < customBarSize; i++) {
            for (int j = 0; j < customBarIndexes; j++) {
                CustomBarLink link = CustomBarkLinkCodec.INSTANCE.read(reader);
                customBar.computeIfAbsent(i, _ -> LinkedHashMap.newLinkedHashMap(customBarSize)).put(j, link);
            }
        }
        StarMVariant activeSlot = starMVariantCodec.read(reader);// Variant<CustomBarIndex, EssentialItem> null, int,int
        StarItemDescriptor beamAxe = StarItemDescriptorCodec.INSTANCE.read(reader);
        StarItemDescriptor wireTool = StarItemDescriptorCodec.INSTANCE.read(reader);
        StarItemDescriptor paintTool = StarItemDescriptorCodec.INSTANCE.read(reader);
        StarItemDescriptor inspectionTool = StarItemDescriptorCodec.INSTANCE.read(reader);
        readPrimaryHandItem(reader);
        return new PlayerInventory(equipment, bags, cursorItem, trashSlot, stringMap, customBarState, customBar, activeSlot, beamAxe, wireTool, paintTool, inspectionTool);
    }

    @Override
    public void write(BinaryWriter writer, PlayerInventory value) {
        final int protocolVersion = writer.openProtocolVersion();
        int mapSize = protocolVersion >= 9 ? 20 : 8;
        for (int i = 0; i < mapSize; i++) {
            EquipmentSlot slot = EquipmentSlot.fromId(i);
            StarItemDescriptor item = value.equipment().get(slot);
            StarItemDescriptorCodec.INSTANCE.write(writer, item);
        }
        final int bagsCount = 5;
        final int bagCapacity = 40;
        for (int i = 0; i < bagsCount; i++) {
            for (int j = 0; j < bagCapacity; j++) {
                StarItemDescriptor item = value.bags().getOrDefault(i, Collections.emptyMap()).get(j);
                StarItemDescriptorCodec.INSTANCE.write(writer, item);
            }
        }
        StarItemDescriptorCodec.INSTANCE.write(writer, value.cursorItem());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.trashSlot());
        int currenciesMapSize = value.currencies().size();
        VlqCodec.INSTANCE.write(writer, currenciesMapSize);
        for (Map.Entry<String, Long> entry : value.currencies().entrySet()) {
            StarStringCodec.INSTANCE.write(writer, entry.getKey());
            writer.writeInt64BE(entry.getValue());
        }
        VlqCodec.INSTANCE.write(writer, value.customBarState());
        final int customBarIndexes = 6;
        final int customBarSize = 2;
        for (int i = 0; i < customBarSize; i++) {
            for (int j = 0; j < customBarIndexes; j++) {
                CustomBarLink link = value.customBar().getOrDefault(i, Collections.emptyMap()).get(j);
                CustomBarkLinkCodec.INSTANCE.write(writer, link);
            }
        }
        starMVariantCodec.write(writer, value.activeSlot());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.beamAxe());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.WireTool());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.paintTool());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.inspectionTool());
    }

    @SuppressWarnings("unused")
    public void readPrimaryHandItem(BinaryReader reader) {
        StarItemDescriptor a = StarItemDescriptorCodec.INSTANCE.read(reader);
        String directives = StarStringCodec.INSTANCE.read(reader);
        float zoom = reader.readFloat32BE();
        boolean flipped = reader.readBoolean();
        float flippedRelativeCenterLine = reader.readFloat32BE();
        float animationRate = reader.readFloat32BE();
        Map<String, String> globalTags = starNetMapCodec.read(reader);
        MapVariantValue parameters = (MapVariantValue) a.parameters();
        var animationParts = (MapVariantValue) parameters.value().get("animationParts");
        if (animationParts != null) {
            Map<String, Map<String, String>> animatedPartsMap = new LinkedHashMap<>();
            for (String partName : animationParts.value().keySet()) {
                Map<String, String> partTags = starNetMapCodec.read(reader);
                animatedPartsMap.put(partName, partTags);
            }
            int l = 1;
        }
        var animationCustom = (MapVariantValue) parameters.value().get("animationCustom");
        if (animationCustom != null) {
            MapVariantValue animatedParts = (MapVariantValue) animationCustom.value().get("animatedParts");
            if (animatedParts != null) {
                MapVariantValue stateTypes = (MapVariantValue) animatedParts.value().get("stateTypes");
                if (stateTypes != null) {
                    Set<String> strings = stateTypes.value().keySet();
                    for (int i = 0; i < strings.size() + 3; i++) {
                        // 1. reverse (NetElementBool) - uint8
                        boolean reverseValue = false;
                        if (reader.openProtocolVersion() >= 10) {
                            reverseValue = reader.readBoolean();
                        }


                        // 2. stateIndex    (NetElementSize) - VLQ с NPos encoding
                        long stateIndexVlq = VlqCodec.INSTANCE.read(reader);

                        // 3. startedEvent (NetElementEvent/NetElementUInt) - VLQ uint64
                        long startedEventCount = VlqCodec.INSTANCE.read(reader);
                    }
                }
            }
            MapVariantValue transformationGroups = (MapVariantValue) animationCustom.value().get("transformationGroups");
            if (transformationGroups != null) {
                for (String groupName : transformationGroups.value().keySet()) {
                    var xTranslation = reader.readFloat32BE();
                    var yTranslation = reader.readFloat32BE();
                    var xScale = reader.readFloat32BE();
                    var yScale = reader.readFloat32BE();
                    var xShear = reader.readFloat32BE();
                    var yShear = reader.readFloat32BE();
                }

            }
            MapVariantValue rotationGroups = (MapVariantValue) animationCustom.value().get("rotationGroups");
            if (rotationGroups != null) {
                for (String groupName : rotationGroups.value().keySet()) {
                    var targetAngle = reader.readFloat32BE();
                    var netImmediateEvent = VlqCodec.INSTANCE.read(reader);
                }
            }
            MapVariantValue particleEmitters = (MapVariantValue) animationCustom.value().get("particleEmitters");
            if (particleEmitters != null) {
                for (String emitterName : particleEmitters.value().keySet()) {
                    var emissionRate = reader.readFloat32BE();
                    var burstCount = reader.readUnsignedByte();
                    var randomSelectCount = reader.readUnsignedByte();
                    var offsetRegion = StarVec2FCodec.INSTANCE.read(reader);
                    var active = reader.readBoolean();
                    var burstEvent = VlqCodec.INSTANCE.read(reader);
                }
            }
        }
        // TODO: Incompleted cuz of the base game package needs. Do it later
    }


}


