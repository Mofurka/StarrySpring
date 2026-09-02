package irden.space.proxy.protocol.packet;

public enum PacketType {
    PROTOCOL_REQUEST(0),
    PROTOCOL_RESPONSE(1),
    SERVER_DISCONNECT(2),
    CONNECT_SUCCESS(3),
    CONNECT_FAILURE(4),
    HANDSHAKE_CHALLENGE(5),
    CHAT_RECEIVE(6),
    UNIVERSE_TIME_UPDATE(7),
    CELESTIAL_RESPONSE(8),
    PLAYER_WARP_RESULT(9),
    PLANET_TYPE_UPDATE(10),
    PAUSE(11),
    SERVER_INFO(12),
    CLIENT_CONNECT(13),
    CLIENT_DISCONNECT_REQUEST(14),
    HANDSHAKE_RESPONSE(15),
    PLAYER_WARP(16),
    FLY_SHIP(17),
    CHAT_SENT(18),
    CELESTIAL_REQUEST(19),
    CLIENT_CONTEXT_UPDATE(20),
    WORLD_START(21),
    WORLD_STOP(22),
    WORLD_LAYOUT_UPDATE(23),
    WORLD_PARAMETERS_UPDATE(24),
    CENTRAL_STRUCTURE_UPDATE(25),
    TILE_ARRAY_UPDATE(26),
    TILE_UPDATE(27),
    TILE_LIQUID_UPDATE(28),
    TILE_DAMAGE_UPDATE(29),
    TILE_MODIFICATION_FAILURE(30),
    GIVE_ITEM(31),
    ENVIRONMENT_UPDATE(32),
    UPDATE_TILE_PROTECTION(33),
    SET_DUNGEON_GRAVITY(34),
    SET_DUNGEON_BREATHABLE(35),
    SET_PLAYER_START(36),
    FIND_UNIQUE_ENTITY_RESPONSE(37),
    PONG(38),
    MODIFY_TILE_LIST(39),
    DAMAGE_TILE_GROUP(40),
    COLLECT_LIQUID(41),
    REQUEST_DROP(42),
    SPAWN_ENTITY(43),
    CONNECT_WIRE(44),
    DISCONNECT_ALL_WIRES(45),
    WORLD_CLIENT_STATE_UPDATE(46),
    FIND_UNIQUE_ENTITY(47),
    WORLD_START_ACKNOWLEDGE(48),
    PING(49),
    ENTITY_CREATE(50),
    ENTITY_UPDATE(51),
    ENTITY_DESTROY(52),
    ENTITY_INTERACT(53),
    ENTITY_INTERACT_RESULT(54),
    HIT_REQUEST(55),
    REMOTE_DAMAGE_REQUEST(56),
    REMOTE_DAMAGE_NOTIFICATION(57),
    ENTITY_MESSAGE(58),
    ENTITY_MESSAGE_RESPONSE(59),
    UPDATE_WORLD_PROPERTIES(60),
    STEP_UPDATE(61),
    SYSTEM_WORLD_START(62),
    SYSTEM_WORLD_UPDATE(63),
    SYSTEM_OBJECT_CREATE(64),
    SYSTEM_OBJECT_DESTROY(65),
    SYSTEM_SHIP_CREATE(66),
    SYSTEM_SHIP_DESTROY(67),
    SYSTEM_OBJECT_SPAWN(68),
    REPLACE_TILE_LIST(69),
    UPDATE_WORLD_TEMPLATE(70);

    private static final PacketType[] BY_ID = buildById();
    private final int id;


    PacketType(int id) {
        this.id = id;
    }

    private static PacketType[] buildById() {
        int maxId = 0;
        for (PacketType type : values()) {
            maxId = Math.max(maxId, type.id);
        }

        PacketType[] byId = new PacketType[maxId + 1];
        for (PacketType type : values()) {
            byId[type.id] = type;
        }
        return byId;
    }

    public static PacketType fromId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return null;
        }
        return BY_ID[id];
    }

    public int id() {
        return id;
    }
}