package irden.space.proxy.protocol.payload.common.warp.target;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinatesCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.packet.warp.consts.WarpWorldType;

public enum WorldTargetCodec implements BinaryCodec<WorldTarget> {
    INSTANCE;

    @Override
    public WorldTarget read (BinaryReader reader) {
        WarpWorldType worldId = WarpWorldType.fromId(reader.readUnsignedByte());

        return switch (worldId) {
            case CELESTIAL_WORLD -> {
                CelestialCoordinates coordinates = CelestialCoordinatesCodec.INSTANCE.read(reader);
                int isTeleporter = reader.readUnsignedByte();
                String teleporter = isTeleporter == 1 ? StarStringCodec.read(reader) : null;
                yield new CelestialWorldTarget(coordinates, teleporter);
            }
            case PLAYER_WORLD -> {
                StarUuid shipUuid = StarUuidCodec.INSTANCE.read(reader);
                int flag = reader.readUnsignedByte();
                Integer posX = null;
                Integer posY = null;
                if (flag == 2) {
                    posX = reader.readInt32BE();
                    posY = reader.readInt32BE();
                }
                yield new PlayerWorldTarget(shipUuid, posX, posY);
            }
            case UNIQUE_WORLD -> {
                String worldName = StarStringCodec.read(reader);

                int isInstance = reader.readUnsignedByte();
                StarUuid instanceUuid = isInstance == 1 ? StarUuidCodec.INSTANCE.read(reader) : null;

                int isSomething = reader.readUnsignedByte();
                Float something = isSomething == 1 ? reader.readFloat32BE() : null;

                int isTeleporter = reader.readUnsignedByte();
                String teleporter = isTeleporter == 1 ? StarStringCodec.read(reader) : null;

                yield new UniqueWorldTarget(worldName, instanceUuid, something, teleporter);
            }
            case MISSION_WORLD -> {
                String worldName = StarStringCodec.read(reader);
                yield new MissionWorldTarget(worldName);
            }
        };

    }

    @Override
    public void write(BinaryWriter writer, WorldTarget target) {
        switch (target) {
            case CelestialWorldTarget(CelestialCoordinates celestialCoordinates, String teleporter) -> {
                writer.writeByte(WarpWorldType.CELESTIAL_WORLD.id());
                CelestialCoordinatesCodec.INSTANCE.write(writer, celestialCoordinates);
                if (teleporter != null) {
                    writer.writeByte(1);
                    StarStringCodec.write(writer, teleporter);
                } else {
                    writer.writeByte(0);
                }
            }
            case PlayerWorldTarget(StarUuid shipUuid, Integer posX, Integer posY) -> {
                writer.writeByte(WarpWorldType.PLAYER_WORLD.id());
                StarUuidCodec.INSTANCE.write(writer, shipUuid);
                if (posX != null && posY != null) {
                    writer.writeByte(2);
                    writer.writeInt32BE(posX);
                    writer.writeInt32BE(posY);
                } else {
                    writer.writeByte(0);
                }
            }
            case UniqueWorldTarget(
                    String worldName, StarUuid instanceUuid, Float something, String teleporter
            ) -> {
                writer.writeByte(WarpWorldType.UNIQUE_WORLD.id());
                StarStringCodec.write(writer, worldName);

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
                    StarStringCodec.write(writer, teleporter);
                } else {
                    writer.writeByte(0);
                }
            }
            case MissionWorldTarget p -> {
                writer.writeByte(WarpWorldType.MISSION_WORLD.id());
                StarStringCodec.write(writer, p.worldName());
            }
            default -> throw new IllegalStateException("Unsupported world target: " + target);
        }
    }

}
