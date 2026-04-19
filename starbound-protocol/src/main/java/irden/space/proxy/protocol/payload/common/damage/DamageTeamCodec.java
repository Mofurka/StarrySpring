package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;

public enum DamageTeamCodec implements BinaryCodec<DamageTeam> {
    INSTANCE;

    @Override
    public DamageTeam read(BinaryReader reader) {
        TeamType teamType = TeamType.fromId(reader.readUnsignedByte());
        short teamNumber = reader.readInt16BE();
        return new DamageTeam(teamType, teamNumber);
    }

    @Override
    public void write(BinaryWriter writer, DamageTeam value) {
        writer.writeByte((byte) value.teamType().id());
        writer.writeInt16BE(value.teamNumber());
    }
}
