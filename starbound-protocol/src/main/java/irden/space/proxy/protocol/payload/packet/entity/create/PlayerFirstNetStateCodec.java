package irden.space.proxy.protocol.payload.packet.entity.create;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.damage.DamageTeamCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.player.HumanoidIdentityCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerInventoryCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;

public enum PlayerFirstNetStateCodec implements BinaryCodec<PlayerNetState> {
    INSTANCE;

    @Override
    @SuppressWarnings("unused")
    public PlayerNetState read(BinaryReader reader) {
        BinaryReader storeDataReader = new BinaryReader(StarByteArrayCodec.INSTANCE.read(reader), reader.openProtocolVersion());
        var netState = PlayerNetState.builder();
        boolean fullUpdate = storeDataReader.readBoolean();
        if (!fullUpdate) {
            throw new IllegalStateException("Expected full update for PlayerFirstNetState");
        }
        netState.state(VlqUnsignedCodec.INSTANCE.read(storeDataReader));
        netState.shifting(storeDataReader.readBoolean());
        StarVec2F mousePos = StarVec2FCodec.INSTANCE.readFixedPointBased(storeDataReader, 0.003125f);
        netState.xMousePos(mousePos.x());
        netState.yMousePos(mousePos.y());
        netState.humanoidIdentity(HumanoidIdentityCodec.INSTANCE.read(storeDataReader));
        netState.damageTeam(DamageTeamCodec.INSTANCE.read(storeDataReader));
        netState.landed(VlqUnsignedCodec.INSTANCE.read(storeDataReader));
        netState.chatMessage(StarStringCodec.INSTANCE.read(storeDataReader));
        netState.newChatMessage(storeDataReader.readBoolean());
        netState.emote(StarStringCodec.INSTANCE.read(storeDataReader));
        netState.inventory(PlayerInventoryCodec.INSTANCE.read(storeDataReader));
        storeDataReader.readRemainingBytes();
        // After inventory there goes tool item. This shit has an NetworkedAnimator as tail if alt or primary item is present in it and it really hard to parse.
        return netState.build();
    }

    @Override
    public void write(BinaryWriter writer, PlayerNetState value) {
        throw new UnsupportedOperationException("PlayerFirstNetState packet is not supported to write.");
    }
}
