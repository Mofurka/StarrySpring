package irden.space.proxy.protocol.payload.packet.world_start;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldStartParser implements PacketParser<WorldStart> {


    @Override
    public WorldStart parse(BinaryReader reader) {
        VariantValue templateData = VariantCodec.INSTANCE.read(reader);
        byte[] skyData = StarByteArrayCodec.INSTANCE.read(reader);
        byte[] weatherData = StarByteArrayCodec.INSTANCE.read(reader);
        StarVec2F playerStart = StarVec2FCodec.INSTANCE.read(reader);
        StarVec2F playerRespawn = StarVec2FCodec.INSTANCE.read(reader);
        boolean respawnInWorld = reader.readBoolean();
        VariantValue worldProperties = VariantCodec.INSTANCE.read(reader);

        Map<Short, Float> dungeonIdGravity = new HashMap<>();
        int size = VlqCodec.INSTANCE.read(reader);
        for (int i = 0; i < size; i++) {
            short dungeonId = reader.readInt16BE(); // DungeonId
            float gravity = reader.readFloat32BE();
            dungeonIdGravity.put(dungeonId, gravity);
        }
        
        Map<Short, Boolean> dungeonIdBreathable = new HashMap<>();
        size = VlqCodec.INSTANCE.read(reader);
        for (int i = 0; i < size; i++) {
            short dungeonId = reader.readInt16BE(); // DungeonId
            boolean breathable = reader.readBoolean();
            dungeonIdBreathable.put(dungeonId, breathable);
        }
        
        List<Short> protectedDungeonIds = new ArrayList<>();
        size = VlqCodec.INSTANCE.read(reader);
        for (int i = 0; i < size; i++) {
            protectedDungeonIds.add(reader.readInt16BE());
        }
        
        int connectionId = reader.readUInt16BE();
        boolean localInterpolationMode = reader.readBoolean();

        return new WorldStart(
                templateData,
                skyData,
                weatherData,
                playerStart,
                playerRespawn,
                respawnInWorld,
                dungeonIdGravity,
                dungeonIdBreathable,
                protectedDungeonIds,
                worldProperties,
                connectionId,
                localInterpolationMode
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, WorldStart payload) {
        VariantCodec.INSTANCE.write(writer, payload.templateData());
        StarByteArrayCodec.INSTANCE.write(writer, payload.skyData());
        StarByteArrayCodec.INSTANCE.write(writer, payload.weatherData());
        StarVec2FCodec.INSTANCE.write(writer, payload.playerStart());
        StarVec2FCodec.INSTANCE.write(writer, payload.playerRespawn());
        writer.writeBoolean(payload.respawnInWorld());
        VariantCodec.INSTANCE.write(writer, payload.worldProperties());

        VlqCodec.INSTANCE.write(writer, payload.dungeonIdGravity().size());
        for (Map.Entry<Short, Float> entry : payload.dungeonIdGravity().entrySet()) {
            writer.writeInt16BE(entry.getKey());
            writer.writeFloat32BE(entry.getValue());
        }

        VlqCodec.INSTANCE.write(writer, payload.dungeonIdBreathable().size());
        for (Map.Entry<Short, Boolean> entry : payload.dungeonIdBreathable().entrySet()) {
            writer.writeInt16BE(entry.getKey());
            writer.writeBoolean(entry.getValue());
        }

        VlqCodec.INSTANCE.write(writer, payload.protectedDungeonIds().size());
        for (Short dungeonId : payload.protectedDungeonIds()) {
            writer.writeInt16BE(dungeonId);
        }

        writer.writeUInt16BE(payload.connectionId());
        writer.writeBoolean(payload.localInterpolationMode());

        return writer.toByteArray();
    }
}
