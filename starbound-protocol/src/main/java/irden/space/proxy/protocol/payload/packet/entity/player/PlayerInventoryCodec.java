package irden.space.proxy.protocol.payload.packet.entity.player;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptorCodec;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariant;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariantCodec;
import irden.space.proxy.protocol.payload.packet.entity.player.custom_bar_link.CustomBarLink;
import irden.space.proxy.protocol.payload.packet.entity.player.custom_bar_link.CustomBarkLinkCodec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum PlayerInventoryCodec implements BinaryCodec<PlayerInventory> {
    INSTANCE;
    private final StarMVariantCodec starMVariantCodec = new StarMVariantCodec(
            VlqUCodec.INSTANCE,
            VlqUCodec.INSTANCE
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

        int currenciesMapSize = VlqUCodec.INSTANCE.read(reader); // currencies
        Map<String, Long> stringMap = LinkedHashMap.newLinkedHashMap(currenciesMapSize);
        for (int i = 0; i < currenciesMapSize; i++) {
            String key = StarStringCodec.INSTANCE.read(reader);
            long ammount = reader.readInt64BE();
            stringMap.put(key, ammount);
        }

        int customBarState = VlqUCodec.INSTANCE.read(reader); // state
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
        VlqUCodec.INSTANCE.write(writer, currenciesMapSize);
        for (Map.Entry<String, Long> entry : value.currencies().entrySet()) {
            StarStringCodec.INSTANCE.write(writer, entry.getKey());
            writer.writeInt64BE(entry.getValue());
        }
        VlqUCodec.INSTANCE.write(writer, value.customBarState());
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
        StarItemDescriptorCodec.INSTANCE.write(writer, value.wireTool());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.paintTool());
        StarItemDescriptorCodec.INSTANCE.write(writer, value.inspectionTool());
    }

/*
    @SuppressWarnings({"unused", "unchecked"})
    public void readPrimaryHandItem(BinaryReader reader) {
        StarItemDescriptor packetItem = StarItemDescriptorCodec.INSTANCE.read(reader);
        if (packetItem.name().isBlank()) return;
        ActiveItem originalItem = GameAssetStores.defaultStore().findItem(packetItem.name()).orElseThrow(() -> new IllegalStateException("ActiveItem not found for name: " + packetItem.name()));
        JsonNode finalItem = MapVariantUtils.merge(originalItem.getData(), (MapVariantValue) packetItem.parameters());
        JsonNode animationPath = finalItem.get("animation");
        JsonNode animationCustom = null;
        if (animationPath != null && animationPath.isTextual()) {
            String textAnimationPath = animationPath.asText();
            String fullAnimationPath;
            if (textAnimationPath.startsWith("/")) {
                fullAnimationPath = textAnimationPath;
            } else {
                fullAnimationPath = originalItem.getItemDirectory().concat("/").concat(textAnimationPath).replace("\\", "/");
            }
            byte[] bytes = GameAssetStores.defaultStore().findAsset(fullAnimationPath).orElseThrow(() -> new IllegalStateException("Active Item animation not found for item: " + originalItem.getItemName() + ", animation path: " + fullAnimationPath));
            JsonNode animationNode;
            try {
                animationNode = objectMapper.readTree(bytes);
            } catch (IOException e) {
                throw new IllegalStateException("Incorrect animation: " + fullAnimationPath);
            }
            JsonNode animationCustom1 = finalItem.get("animationCustom");
            if (animationCustom1 != null && animationCustom1.isObject()) {
                animationCustom = JsonUtils.merge(animationNode, animationCustom1);
            } else {
                animationCustom = animationNode;
            }
        }
        var originalItemAnimationCustom = originalItem.get("animationCustom");
        if (originalItemAnimationCustom != null && originalItemAnimationCustom.isObject()) {
            animationCustom = JsonUtils.merge(originalItemAnimationCustom, animationCustom);
        }


        String directives = StarStringCodec.INSTANCE.read(reader);
        float zoom = reader.readFloat32BE();
        boolean flipped = reader.readBoolean();
        float flippedRelativeCenterLine = reader.readFloat32BE();
        float animationRate = reader.readFloat32BE();
        Map<String, String> globalTags = starNetMapCodec.read(reader);
        var animationParts = animationCustom.get("animatedParts").get("parts");
        if (animationParts != null) {
            Map<String, Map<String, String>> animatedPartsMap = new LinkedHashMap<>();
            for (Iterator<String> it = animationParts.fieldNames(); it.hasNext(); ) {
                String partName = it.next();
                Map<String, String> partTags = starNetMapCodec.read(reader);
                animatedPartsMap.put(partName, partTags);
            }
        }

        var animatedParts = animationCustom.get("animatedParts");
        if (animatedParts != null) {
            var stateTypes = animatedParts.get("stateTypes");
            if (stateTypes != null) {
                Iterator<String> strings = stateTypes.fieldNames();
                for (String str; strings.hasNext(); ) {
                    strings.next();
                    boolean reverseValue = false;
                    if (reader.openProtocolVersion() >= 10) {
//                        reverseValue = reader.readBoolean();
                    }


                    var stateIndexVlq = VlqCodec.INSTANCE.read(reader);

                    boolean startedEvent = reader.readBoolean();
                    int l = 1;
                }
            }


            var transformationGroups = animationCustom.get("transformationGroups");
            if (transformationGroups != null) {
                for (Iterator<String> it = transformationGroups.fieldNames(); it.hasNext(); ) {
                    String groupName = it.next();
                    var xTranslation = reader.readFloat32BE();
                    var yTranslation = reader.readFloat32BE();
                    var xScale = reader.readFloat32BE();
                    var yScale = reader.readFloat32BE();
                    var xShear = reader.readFloat32BE();
                    var yShear = reader.readFloat32BE();
                }

            }
            var rotationGroups = animationCustom.get("rotationGroups");
            if (rotationGroups != null) {
                for (Iterator<String> it = rotationGroups.fieldNames(); it.hasNext(); ) {
                    String groupName = it.next();
                    var targetAngle = reader.readFloat32BE();
                    var netImmediateEvent = VlqUCodec.INSTANCE.read(reader);
                }
            }
            var particleEmitters = animationCustom.get("particleEmitters");
            if (particleEmitters != null) {
                for (Iterator<String> it = particleEmitters.fieldNames(); it.hasNext(); ) {
                    String emitterName = it.next();
                    var emissionRate = reader.readFloat32BE();
                    var burstCount = reader.readUnsignedByte();
                    var randomSelectCount = reader.readUnsignedByte();
                    var offsetRegion = StarRect4FCodec.INSTANCE.read(reader);
                    var active = reader.readBoolean();
                    var burstEvent = VlqUCodec.INSTANCE.read(reader);
                    int l = 1;
                }
            }

            var lights = animationCustom.get("lights");
            if (lights != null) {
                for (Iterator<String> it = lights.fieldNames(); it.hasNext(); ) {
                    String lightName = it.next();
                    var active = reader.readBoolean();
                    var offset = StarVec2FCodec.INSTANCE.readFixedPointBased(reader, 0.0125f);
                    var color = StarRect4FCodec.INSTANCE.read(reader);
                    var pointAngle = VlqCodec.INSTANCE.read(reader) * 0.01f;
                    int l = 1;
                }
            }

            var sounds = animationCustom.get("sounds");
            if (sounds != null) {
                for (Iterator<String> it = sounds.fieldNames(); it.hasNext(); ) {
                    it.next();
                    Integer stringListSize = VlqUCodec.INSTANCE.read(reader);
                    List<String> stringList = new ArrayList<>(stringListSize);
                    for (int i = 0; i < stringListSize; i++) {
                        String s = StarStringCodec.INSTANCE.read(reader);
                        stringList.add(s);
                    }
                    StarVec2F starVec2F = StarVec2FCodec.INSTANCE.readFixedPointBased(reader, 0.0125f);
                    var volumeTarget = reader.readFloat32BE();
                    var volumeRampTime = reader.readFloat32BE();
                    var pitchMultiplierTarget = reader.readFloat32BE();
                    var pitchMultiplierRampTime = reader.readFloat32BE();
                    var loopCount = VlqCodec.INSTANCE.read(reader);
                    int l = 1;
                }
            }


            var effects = animationCustom.get("effects");
            if (effects != null) {
                for (Iterator<String> it = effects.fieldNames(); it.hasNext(); ) {
                    String effectName = it.next();
                    var enabled = reader.readBoolean();
                }
            }

            var holdingItem = reader.readBoolean();
            var backArmFrame = stringMaybe.read(reader);
            var frontArmFrame = stringMaybe.read(reader);
            var twoHanded = reader.readBoolean();
            var recoil = reader.readBoolean();
            var outsideOfHand = reader.readBoolean();
            var armAngle = reader.readFloat32BE();
            // Incompleted implemetation. A lot of variant shit

        }
    }

*/


}


