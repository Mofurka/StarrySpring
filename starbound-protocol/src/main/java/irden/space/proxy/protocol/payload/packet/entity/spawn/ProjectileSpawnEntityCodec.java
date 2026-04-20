package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.damage.DamageTeamCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.ProjectileEntity;

public enum ProjectileSpawnEntityCodec implements BinaryCodec<ProjectileEntity> {
    INSTANCE;

    @Override
    public ProjectileEntity read(BinaryReader reader) {
        var name = StarStringCodec.INSTANCE.read(reader);
        var parameters = VariantCodec.INSTANCE.read(reader);
        var entityId = VlqUnsignedCodec.INSTANCE.read(reader);
        var trackSourceEntity = reader.readBoolean();
        var initialSpeed = reader.readFloat32BE();
        var powerMultiplier = reader.readFloat32BE();
        var damageTeam = DamageTeamCodec.INSTANCE.read(reader);
        return new ProjectileEntity(name, parameters, entityId, trackSourceEntity, initialSpeed, powerMultiplier, damageTeam);
    }

    @Override
    public void write(BinaryWriter writer, ProjectileEntity value) {

    }
}
