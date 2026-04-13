package irden.space.proxy.protocol.payload.common.warp.action;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.common.warp.target.WorldTarget;
import irden.space.proxy.protocol.payload.common.warp.target.WorldTargetCodec;
import irden.space.proxy.protocol.payload.packet.warp.consts.WarpType;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class WarpActionCodec {

    public static WarpAction read(BinaryReader reader) {
        WarpType warpType = WarpType.fromId(reader.readUnsignedByte());
        return switch (warpType) {
            case TO_WORLD -> new ToWorldWarpAction(WorldTargetCodec.read(reader));
            case TO_PLAYER -> new ToPlayerWarpAction(StarUuidCodec.read(reader));
            case TO_ALIAS -> new ToAliasWarpAction(reader.readInt32BE());
        };
    }

    public static void write(BinaryWriter writer, WarpAction payload) {
        switch (payload) {
            case ToWorldWarpAction(WorldTarget target) -> {
                writer.writeByte(WarpType.TO_WORLD.id());
                WorldTargetCodec.write(writer, target);
            }
            case ToPlayerWarpAction(StarUuid uuid) -> {
                writer.writeByte(WarpType.TO_PLAYER.id());
                StarUuidCodec.write(writer, uuid);
            }
            case ToAliasWarpAction(int aliasId) -> {
                writer.writeByte(WarpType.TO_ALIAS.id());
                writer.writeInt32BE(aliasId);
            }
            default -> throw new IllegalStateException("Unsupported warp payload: " + payload);
        }
    }




}
