package irden.space.proxy.protocol.packet;

import lombok.Getter;

public enum PacketType {
    PROTOCOL_REQUEST(0),
    PROTOCOL_RESPONSE(1),
    SERVER_DISCONNECT(2),
    CONNECT_SUCCESS(3),
    CONNECT_FAILURE(4),
    HANDSHAKE_CHALLENGE(5),
    CHAT_RECEIVED(6),
    NONE_7(7),
    NONE_8(8),
    PLAYER_WARP_RESULT(9),
    NONE_10(10),
    NONE_11(11),
    NONE_12(12),
    CLIENT_CONNECT(13),
    CLIENT_DISCONNECT_REQUEST(14),
    NONE_15(15),
    PLAYER_WARP(16),
    FLY_SHIP(17),
    CHAT_SENT(18),
    NONE_19(19),
    NONE_20(20),
    WORLD_START(21),
    WORLD_STOP(22),
    NONE_23(23),
    NONE_24(24),
    NONE_25(25),
    NONE_26(26),
    NONE_27(27),
    NONE_28(28),
    NONE_29(29),
    NONE_30(30),
    GIVE_ITEM(31),
    ENVIRONMENT_UPDATE(32),
    NONE_33(33),
    NONE_34(34),
    NONE_35(35),
    NONE_36(36),
    NONE_37(37),
    NONE_38(38),
    MODIFY_TILE_LIST(39),
    NONE_40(40),
    NONE_41(41),
    NONE_42(42),
    SPAWN_ENTITY(43),
    NONE_44(44),
    NONE_45(45),
    NONE_46(46),
    NONE_47(47),
    NONE_48(48),
    NONE_49(49),
    ENTITY_CREATE(50),
    ENTITY_UPDATE(51),
    ENTITY_DESTROY(52),
    ENTITY_INTERACT(53),
    ENTITY_INTERACT_RESULT(54),
    NONE_55(55),
    DAMAGE_REQUEST(56),
    DAMAGE_NOTIFICATION(57),
    ENTITY_MESSAGE(58),
    ENTITY_MESSAGE_RESPONSE(59),
    NONE_60(60),
    STEP_UPDATE(61),
    NONE_62(62),
    NONE_63(63),
    NONE_64(64),
    NONE_65(65),
    NONE_66(66),
    NONE_67(67),
    NONE_68(68);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}