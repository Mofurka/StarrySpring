package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinatesCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.packet.warp.consts.SpawnTarget;
import irden.space.proxy.protocol.payload.packet.warp.consts.WarpWorldType;

public enum WorldTargetCodec implements BinaryCodec<WorldTarget> {
    INSTANCE;

    @Override
    public WorldTarget read(BinaryReader reader) {
        WarpWorldType worldId = WarpWorldType.fromId(reader.readUnsignedByte());

        return switch (worldId) {
            case CELESTIAL_WORLD -> {
                CelestialCoordinates coordinates = CelestialCoordinatesCodec.INSTANCE.read(reader);
                SpawnTarget spawnTarget = parseSpawnTarget(reader);
                yield new CelestialWorldTarget(coordinates, spawnTarget);
            }

            case PLAYER_WORLD -> {
                StarUuid shipUuid = StarUuidCodec.INSTANCE.read(reader);
                SpawnTarget spawnTarget = parseSpawnTarget(reader);
                yield new PlayerWorldTarget(shipUuid, spawnTarget);
            }
            case UNIQUE_WORLD -> {
                String worldName = StarStringCodec.INSTANCE.read(reader);

                int isInstance = reader.readUnsignedByte();
                StarUuid instanceUuid = isInstance == 1 ? StarUuidCodec.INSTANCE.read(reader) : null;

                int isSomething = reader.readUnsignedByte();
                Float something = isSomething == 1 ? reader.readFloat32BE() : null;

                int isTeleporter = reader.readUnsignedByte();
                String teleporter = isTeleporter == 1 ? StarStringCodec.INSTANCE.read(reader) : null;

                yield new UniqueWorldTarget(worldName, instanceUuid, something, teleporter);
            }
            case MISSION_WORLD -> {
                String worldName = StarStringCodec.INSTANCE.read(reader);
                yield new MissionWorldTarget(worldName);
            }
        };
    }

    @Override
    public void write(BinaryWriter writer, WorldTarget target) {
        switch (target) {
            case CelestialWorldTarget(CelestialCoordinates celestialCoordinates, SpawnTarget target1) -> {
                writer.writeByte(WarpWorldType.CELESTIAL_WORLD.id());
                CelestialCoordinatesCodec.INSTANCE.write(writer, celestialCoordinates);
                writeSpawnTarget(writer, target1);
            }
            case PlayerWorldTarget(StarUuid shipUuid, SpawnTarget spawnTarget) -> {
                writer.writeByte(WarpWorldType.PLAYER_WORLD.id());
                StarUuidCodec.INSTANCE.write(writer, shipUuid);
                writeSpawnTarget(writer, spawnTarget);
            }
            case UniqueWorldTarget(
                    String worldName, StarUuid instanceUuid, Float something, String teleporter
            ) -> {
                writer.writeByte(WarpWorldType.UNIQUE_WORLD.id());
                StarStringCodec.INSTANCE.write(writer, worldName);

                if (instanceUuid != null) {
                    writer.writeByte(1);
                    StarUuidCodec.INSTANCE.write(writer, instanceUuid);
                } else {
                    writer.writeByte(0);
                }

                if (something != null) {
                    writer.writeByte(1);
                    writer.writeFloat32BE(something);
                } else {
                    writer.writeByte(0);
                }

                if (teleporter != null) {
                    writer.writeByte(1);
                    StarStringCodec.INSTANCE.write(writer, teleporter);
                } else {
                    writer.writeByte(0);
                }
            }
            case MissionWorldTarget(String worldName) -> {
                writer.writeByte(WarpWorldType.MISSION_WORLD.id());
                StarStringCodec.INSTANCE.write(writer, worldName);
            }
            default -> throw new IllegalStateException("Unsupported world target: " + target);
        }
    }

    public SpawnTarget parseSpawnTarget(BinaryReader reader) {
        int spawnTargetType = reader.readUnsignedByte();
        return switch (spawnTargetType) {
            case 0 -> null;
            case 1 -> new SpawnTarget.UniqueEntity(StarStringCodec.INSTANCE.read(reader));
            case 2 -> new SpawnTarget.Position(StarVec2FCodec.INSTANCE.read(reader));
            case 3 -> new SpawnTarget.XCoordinate(reader.readFloat32BE());
            default -> throw new IllegalStateException("Unexpected value: " + spawnTargetType);
        };
    }

    public void writeSpawnTarget(BinaryWriter writer, SpawnTarget target) {
        switch (target) {
            case null -> writer.writeByte(0);
            case SpawnTarget.UniqueEntity(String entityName) -> {
                writer.writeByte(1);
                StarStringCodec.INSTANCE.write(writer, entityName);
            }
            case SpawnTarget.Position(StarVec2F position) -> {
                writer.writeByte(2);
                StarVec2FCodec.INSTANCE.write(writer, position);
            }
            case SpawnTarget.XCoordinate(float x) -> {
                writer.writeByte(3);
                writer.writeFloat32BE(x);
            }
            default -> throw new IllegalStateException("Unsupported spawn target: " + target);
        }
    }

}
