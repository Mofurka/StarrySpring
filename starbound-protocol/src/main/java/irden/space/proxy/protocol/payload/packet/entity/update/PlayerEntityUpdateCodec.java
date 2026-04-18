package irden.space.proxy.protocol.payload.packet.entity.update;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptorCodec;
import irden.space.proxy.protocol.payload.common.star_m_variant.StarMVariantCodec;
import irden.space.proxy.protocol.payload.common.star_map.StarNetMapCodec;
import irden.space.proxy.protocol.payload.common.star_pair.StarPair;
import irden.space.proxy.protocol.payload.common.star_poly.StarPolyFCodec;
import irden.space.proxy.protocol.payload.packet.entity.player.EquipmentSlot;
import irden.space.proxy.protocol.payload.packet.entity.player.HumanoidIdentityCodec;
import irden.space.proxy.protocol.payload.packet.entity.player.MovementController;
import irden.space.proxy.protocol.payload.packet.entity.player.PlayerInventory;
import irden.space.proxy.protocol.payload.packet.entity.player.custom_bar_link.CustomBarLink;
import irden.space.proxy.protocol.payload.packet.entity.player.custom_bar_link.CustomBarkLinkCodec;

import java.nio.charset.StandardCharsets;
import java.util.*;

public enum PlayerEntityUpdateCodec implements BinaryCodec<PlayerUpdateNetState> {
    INSTANCE;
    private final StarMVariantCodec starMVariantCodec = new StarMVariantCodec(
            VlqUCodec.INSTANCE,
            VlqUCodec.INSTANCE
    );
    private final StarNetMapCodec<String, byte[]> starNetMapCodec = new StarNetMapCodec<>(
            StarStringCodec.INSTANCE,
            StarByteArrayCodec.INSTANCE
    );
    private final String magicProperty = "\0JsonProperty\0";

    @Override
    public PlayerUpdateNetState read(BinaryReader reader) {
        throw new UnsupportedOperationException("Use read method with BinaryReader parameter to read player entity update, because it has a complex structure and requires special handling");
    }

    public PlayerUpdateNetState read(BinaryReader reader, PlayerUpdateNetState.PlayerUpdateNetStateBuilder player) {
        // Это мапа дельта-обновлений, которая может содержать любые поля из PlayerEntityCreateCodec, но не все сразу (по сути, это патч для сущности)
        var fullUpdate = reader.readBoolean();
        if (fullUpdate) {
            throw new UnsupportedOperationException("Full player entity update is not supported in entity update packet, only in entity create packet");
        }
        updateLoop:
        while (reader.hasRemaining()) {
            var magicNumber = VlqUCodec.INSTANCE.read(reader);
            if (magicNumber == 0) {
                break;
            }
            switch (magicNumber) {
                case 1 -> player.state(VlqUCodec.INSTANCE.read(reader));
                case 2 -> player.shifting(reader.readBoolean());
                case 3 -> player.xMousePos(VlqCodec.INSTANCE.read(reader) * 0.003125f);
                case 4 -> player.yMousePos(VlqCodec.INSTANCE.read(reader) * 0.003125f);
                case 5 -> player.humanoidIdentity(HumanoidIdentityCodec.INSTANCE.read(reader));
                case 6 -> player.teamType(TeamType.fromId(reader.readUnsignedByte())).teamNumber(reader.readInt16BE());
                case 7 -> player.landed(reader.readBoolean());
                case 8 -> player.chatMessage(StarStringCodec.INSTANCE.read(reader));
                case 9 -> player.newChatMessage(reader.readBoolean());
                case 10 -> player.emote(StarStringCodec.INSTANCE.read(reader));
                case 11 -> player.inventory(this.readInventory(reader));
                case 12 ->
                        reader.readRemainingBytes(); // ХУЙНЯ с очень большим хвостом после. Слишком много параметров, что даже смысла нет парсить.
                case 13 -> reader.readRemainingBytes(); // Armor, not implemented yet
                case 14 -> reader.readRemainingBytes(); // Songbook, not implemented yet
                case 15 -> player.movementController(this.readMcontroller(reader));
                case 16 -> player.effectEmitters(readEffectEmitter(reader)); // Effect emitter, not implemented yet
                case 17 -> player.effectsAnimator(readEffectsAnimator(reader));
//                case 18 -> reader.readRemainingBytes(); // techController - not implemented yet
                default -> {
                    reader.readRemainingBytes();
                    break updateLoop;
                }
            }

        }
        return player.build();
    }

    @Override
    public void write(BinaryWriter writer, PlayerUpdateNetState value) {
        BinaryWriter newWriter = new BinaryWriter(writer.openProtocolVersion());
        newWriter.writeBoolean(false); // full update
        if (value.state() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 1);
            VlqUCodec.INSTANCE.write(newWriter, value.state());
        }
        if (Boolean.TRUE.equals(value.shifting())) {
            VlqUCodec.INSTANCE.write(newWriter, 2);
            newWriter.writeBoolean(true);
        }
        if (value.xMousePos() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 3);
            VlqCodec.INSTANCE.write(newWriter, (int) (value.xMousePos() / 0.003125f));
        }
        if (value.yMousePos() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 4);
            VlqCodec.INSTANCE.write(newWriter, (int) (value.yMousePos() / 0.003125f));
        }
        if (value.humanoidIdentity() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 5);
            HumanoidIdentityCodec.INSTANCE.write(newWriter, value.humanoidIdentity());
        }
        if (value.teamType() != null && value.teamNumber() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 6);
            newWriter.writeByte(value.teamType().id());
            newWriter.writeInt16BE(value.teamNumber());
        }
        if (value.landed() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 7);
            newWriter.writeBoolean(value.landed());
        }
        if (value.chatMessage() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 8);
            StarStringCodec.INSTANCE.write(newWriter, value.chatMessage());
        }
        if (value.newChatMessage() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 9);
            newWriter.writeBoolean(value.newChatMessage());
        }
        if (value.emote() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 10);
            StarStringCodec.INSTANCE.write(newWriter, value.emote());
        }
        if (value.inventory() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 11);
            throw new UnsupportedOperationException("Writing player inventory is not supported yet, too many parameters and complexity");
        }
        if (value.movementController() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 15);
            throw new UnsupportedOperationException("Writing movement controller is not supported yet, too many parameters and complexity");
        }
        if (value.effectEmitters() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 16);
            throw new UnsupportedOperationException("Writing effect emitters is not supported yet, too many parameters and complexity");
        }
        if (value.effectsAnimator() != null) {
            VlqUCodec.INSTANCE.write(newWriter, 17);
            this.writeEffectsAnimator(newWriter, value.effectsAnimator());
        }
        VlqUCodec.INSTANCE.write(newWriter, 0);
        StarByteArrayCodec.INSTANCE.write(writer, newWriter.toByteArray());
    }

    // todo раскидать всё по кодекам, а не держать в одном.
    public MovementController readMcontroller(BinaryReader reader) {
        var mc = MovementController.builder();
        while (true) {
            int magicNumber = VlqUCodec.INSTANCE.read(reader);
            if (magicNumber == 0 || magicNumber > 16) {
                break;
            }
            switch (magicNumber) {
                case 1 -> mc.collisionPoly(StarPolyFCodec.INSTANCE.read(reader));
                case 2 -> mc.mass(reader.readFloat32BE());
                case 3 -> mc.xPosition(VlqCodec.INSTANCE.read(reader) * 0.0125f);
                case 4 -> mc.yPosition(VlqCodec.INSTANCE.read(reader) * 0.0125f);
                case 5 -> mc.xVelocity(VlqCodec.INSTANCE.read(reader) * 0.00625f);
                case 6 -> mc.yVelocity(VlqCodec.INSTANCE.read(reader) * 0.00625f);
                case 7 -> mc.rotation(VlqCodec.INSTANCE.read(reader) * 0.01f);
                case 8 -> mc.colliding(reader.readBoolean());
                case 9 -> mc.collisionStuck(reader.readBoolean());
                case 10 -> mc.nullColliding(reader.readBoolean());
                case 11 -> mc.stickingDirection(
                        reader.readBoolean() ? Optional.of(reader.readFloat32BE()) : Optional.empty()
                );
                case 12 -> mc.onGround(reader.readBoolean());
                case 13 -> mc.zeroG(reader.readBoolean());
                case 14 -> mc.surfaceMovingCollision(
                        this.readSurfaceMovingCollision(reader
                        ));
                case 15 -> mc.xRelativeSurfaceMovingCollisionPosition(reader.readFloat32BE());
                case 16 -> mc.yRelativeSurfaceMovingCollisionPosition(reader.readFloat32BE());
                default -> throw new IllegalStateException("Unexpected value: " + magicNumber);
            }
        }
        return mc.build();
    }

    private Optional<StarPair<Integer, Integer>> readSurfaceMovingCollision(BinaryReader reader) {
        if (reader.readBoolean()) {
            var entityId = reader.readInt32BE();
            var collisionIndex = VlqUCodec.INSTANCE.read(reader);
            return Optional.of(new StarPair<>(entityId, collisionIndex));
        } else {
            return Optional.empty();
        }

    }

    public PlayerInventory readInventory(BinaryReader reader) {
        var pi = PlayerInventory.builder();

        final int protocolVersion = reader.openProtocolVersion();
        final int equipmentEnd = protocolVersion >= 9 ? 20 : 8;
        final int bagsCount = 5;
        final int bagCapacity = 40;
        final int customBarGroups = 2;
        final int customBarIndexes = 6;


        int bagsEnd = equipmentEnd + (bagsCount * bagCapacity);     // 21-220
        int swapSlotIndex = bagsEnd + 1;                            // 221
        int trashSlotIndex = bagsEnd + 2;                           // 222
        int currenciesIndex = bagsEnd + 3;                          // 223
        int customBarGroupIndex = bagsEnd + 4;                      // 224
        int customBarEnd = customBarGroupIndex + (customBarGroups * customBarIndexes); // 225-236
        int selectedActionBarIndex = customBarEnd + 1;              // 237
        int beamAxeIndex = selectedActionBarIndex + 1;              // 238
        int wireToolIndex = beamAxeIndex + 1;                       // 239
        int paintToolIndex = wireToolIndex + 1;                     // 240
        int inspectionToolIndex = paintToolIndex + 1;               // 241

        while (true) {
            int magicNumber = VlqUCodec.INSTANCE.read(reader);
            if (magicNumber == 0) {
                break;
            }

            // Equipment slots (1-20 или 1-8)
            if (magicNumber >= 1 && magicNumber <= equipmentEnd) {
                int slotIndex = magicNumber - 1;
                EquipmentSlot slot = EquipmentSlot.fromId(slotIndex);
                StarItemDescriptor item = StarItemDescriptorCodec.INSTANCE.read(reader);
                pi.equipment(Map.of(slot, item));
            }
            // Bags (21-220)
            else if (magicNumber > equipmentEnd && magicNumber <= bagsEnd) {
                int offset = magicNumber - equipmentEnd - 1;
                int bagIndex = offset / bagCapacity;
                int slotIndex = offset % bagCapacity;
                StarItemDescriptor item = StarItemDescriptorCodec.INSTANCE.read(reader);
                pi.bags(Map.of(bagIndex, Map.of(slotIndex, item)));
            }
            // Swap slot (221)
            else if (magicNumber == swapSlotIndex) {
                pi.cursorItem(StarItemDescriptorCodec.INSTANCE.read(reader));
            }
            // Trash slot (222)
            else if (magicNumber == trashSlotIndex) {
                pi.trashSlot(StarItemDescriptorCodec.INSTANCE.read(reader));
            }
            // Currencies (223)
            else if (magicNumber == currenciesIndex) {
                int currenciesMapSize = VlqUCodec.INSTANCE.read(reader);
                Map<String, Long> currencies = LinkedHashMap.newLinkedHashMap(currenciesMapSize);
                for (int i = 0; i < currenciesMapSize; i++) {
                    String key = StarStringCodec.INSTANCE.read(reader);
                    long amount = reader.readInt64BE();
                    currencies.put(key, amount);
                }
                pi.currencies(currencies);
            }
            // Custom bar group (224)
            else if (magicNumber == customBarGroupIndex) {
                pi.customBarState(VlqUCodec.INSTANCE.read(reader));
            }
            // Custom bar slots (225-236)
            else if (magicNumber > customBarGroupIndex && magicNumber <= customBarEnd) {
                int offset = magicNumber - customBarGroupIndex - 1;
                int groupIndex = offset / customBarIndexes;
                int barIndex = offset % customBarIndexes;
                CustomBarLink link = CustomBarkLinkCodec.INSTANCE.read(reader);
                pi.customBar(Map.of(groupIndex, Map.of(barIndex, link)));
            }
            // Selected action bar (237)
            else if (magicNumber == selectedActionBarIndex) {
                pi.activeSlot(starMVariantCodec.read(reader));
            }
            // Essential items (238-241)
            else if (magicNumber == beamAxeIndex) {
                pi.beamAxe(StarItemDescriptorCodec.INSTANCE.read(reader));
            } else if (magicNumber == wireToolIndex) {
                pi.wireTool(StarItemDescriptorCodec.INSTANCE.read(reader));
            } else if (magicNumber == paintToolIndex) {
                pi.paintTool(StarItemDescriptorCodec.INSTANCE.read(reader));
            } else if (magicNumber == inspectionToolIndex) {
                pi.inspectionTool(StarItemDescriptorCodec.INSTANCE.read(reader));
            } else {
                throw new IllegalStateException("Unknown inventory magic number: " + magicNumber);
            }
        }

        return pi.build();
    }

    public List<StarPair<String, String>> readEffectEmitter(BinaryReader reader) {
        int listSize = VlqUCodec.INSTANCE.read(reader);
        List<StarPair<String, String>> starPairList = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            String key = StarStringCodec.INSTANCE.read(reader);
            String value = StarStringCodec.INSTANCE.read(reader);
            starPairList.add(new StarPair<>(key, value));
        }
        return starPairList;
    }

    public EffectsAnimator readEffectsAnimator(BinaryReader reader) {
        var ea = EffectsAnimator.builder();
        while (reader.hasRemaining()) {
            int magicNumber = VlqUCodec.INSTANCE.read(reader);
            if (magicNumber == 0) {
                break;
            }
            switch (magicNumber) {
                case 1 -> ea.processingDirectives(StarStringCodec.INSTANCE.read(reader));
                case 2 -> ea.zoom(reader.readFloat32BE());
                case 3 -> ea.flipped(reader.readBoolean());
                case 4 -> ea.flippedRelativeCenterLine(reader.readFloat32BE());
                case 5 -> ea.animationRate(reader.readFloat32BE());
                case 6 -> {
                    var globalTags = starNetMapCodec.readDelta(reader);
                    globalTags.forEach((k, v) -> {
                        if (k.startsWith(magicProperty)) {
                            String key = k.substring(magicProperty.length());
                            VariantValue value = VariantCodec.INSTANCE.read(new BinaryReader(v));
                            ea.globalTags(Map.of(key, value));
                        } else {
                            String value = new String(v, StandardCharsets.UTF_8);
                            ea.globalTags(Map.of(k, value));
                        }
                    });
                }
                default -> reader.readRemainingBytes();
            }
        }
        return ea.build();
    }

    public void writeEffectsAnimator(BinaryWriter writer, EffectsAnimator value) {
        if (value.processingDirectives() != null) {
            VlqUCodec.INSTANCE.write(writer, 1);
            StarStringCodec.INSTANCE.write(writer, value.processingDirectives());
        }
        if (value.zoom() != null) {
            VlqUCodec.INSTANCE.write(writer, 2);
            writer.writeFloat32BE(value.zoom());
        }
        if (value.flipped() != null) {
            VlqUCodec.INSTANCE.write(writer, 3);
            writer.writeBoolean(value.flipped());
        }
        if (value.flippedRelativeCenterLine() != null) {
            VlqUCodec.INSTANCE.write(writer, 4);
            writer.writeFloat32BE(value.flippedRelativeCenterLine());
        }
        if (value.animationRate() != null) {
            VlqUCodec.INSTANCE.write(writer, 5);
            writer.writeFloat32BE(value.animationRate());
        }
        if (value.globalTags() != null) {
            VlqUCodec.INSTANCE.write(writer, 6);
            Map<String, byte[]> globalTags = new LinkedHashMap<>();
            value.globalTags().forEach((k, v) -> {
                switch (v) {
                    case String s -> globalTags.put(k, s.getBytes(StandardCharsets.UTF_8));
                    case VariantValue vv -> {
                        BinaryWriter baw = new BinaryWriter();
                        VariantCodec.INSTANCE.write(baw, vv);
                        globalTags.put(magicProperty + k, baw.toByteArray());
                    }
                    default -> throw new IllegalStateException("Unsupported global tag value type: " + v.getClass());

                }
            });
            starNetMapCodec.writeFullReloadDelta(writer, globalTags);
        }
        VlqUCodec.INSTANCE.write(writer, 0);
    }
}
