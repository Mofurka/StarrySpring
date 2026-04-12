package irden.space.proxy.protocol.payload.registry;

import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatRecieveParser;
import irden.space.proxy.protocol.payload.packet.chat.ChatSentParser;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnectParser;
import irden.space.proxy.protocol.payload.packet.connect_failure.ConnectFailureParser;
import irden.space.proxy.protocol.payload.packet.connect_success.ConnectSuccessParser;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessageParser;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageResponseParser;
import irden.space.proxy.protocol.payload.packet.handshake_challenge.HandshakeChallengeParser;
import irden.space.proxy.protocol.payload.packet.pause.PauseParser;
import irden.space.proxy.protocol.payload.packet.protocol_request.ProtocolRequestParser;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponseParser;
import irden.space.proxy.protocol.payload.packet.server_disconnect.ServerDisconnectParser;
import irden.space.proxy.protocol.payload.packet.step_update.StepUpdateParser;
import irden.space.proxy.protocol.payload.packet.universe_time_update.UniverseTimeUpdateParser;
import irden.space.proxy.protocol.payload.packet.warp.player_warp.PlayerWarpParser;
import irden.space.proxy.protocol.payload.packet.warp.player_warp_result.PlayerWarpResultParser;

import java.util.EnumMap;
import java.util.Map;

public class PacketParserRegistry {

    private final Map<PacketType, PacketParser<?>> parsers = new EnumMap<>(PacketType.class);

    public PacketParserRegistry() {
        register(PacketType.PROTOCOL_REQUEST, new ProtocolRequestParser());
        register(PacketType.PROTOCOL_RESPONSE, new ProtocolResponseParser());
        register(PacketType.SERVER_DISCONNECT, new ServerDisconnectParser());
        register(PacketType.CONNECT_SUCCESS, new ConnectSuccessParser());
        register(PacketType.CONNECT_FAILURE, new ConnectFailureParser());
        register(PacketType.HANDSHAKE_CHALLENGE, new HandshakeChallengeParser());
        register(PacketType.CHAT_RECEIVE, new ChatRecieveParser());
        register(PacketType.UNIVERSE_TIME_UPDATE, new UniverseTimeUpdateParser());
        register(PacketType.CELESTIAL_RESPONSE, null);
        register(PacketType.PLAYER_WARP_RESULT, new PlayerWarpResultParser());
        register(PacketType.PLANET_TYPE_UPDATE, null);
        register(PacketType.PAUSE, new PauseParser());
        register(PacketType.SERVER_INFO, null);
        register(PacketType.CLIENT_CONNECT, new ClientConnectParser());
        register(PacketType.CLIENT_DISCONNECT_REQUEST, null);
        register(PacketType.HANDSHAKE_RESPONSE, null);
        register(PacketType.PLAYER_WARP, new PlayerWarpParser());
        register(PacketType.FLY_SHIP, null);
        register(PacketType.CHAT_SENT, new ChatSentParser());
        register(PacketType.CELESTIAL_REQUEST, null);
        register(PacketType.CLIENT_CONTEXT_UPDATE, null);
        register(PacketType.WORLD_START, null);
        register(PacketType.WORLD_STOP, null);
        register(PacketType.WORLD_LAYOUT_UPDATE, null);
        register(PacketType.WORLD_PARAMETERS_UPDATE, null);
        register(PacketType.CENTRAL_STRUCTURE_UPDATE, null);
        register(PacketType.TILE_ARRAY_UPDATE, null);
        register(PacketType.TILE_UPDATE, null);
        register(PacketType.TILE_LIQUID_UPDATE, null);
        register(PacketType.TILE_DAMAGE_UPDATE, null);
        register(PacketType.TILE_MODIFICATION_FAILURE, null);
        register(PacketType.GIVE_ITEM, null);
        register(PacketType.ENVIRONMENT_UPDATE, null);
        register(PacketType.UPDATE_TILE_PROTECTION, null);
        register(PacketType.SET_DUNGEON_GRAVITY, null);
        register(PacketType.SET_DUNGEON_BREATHABLE, null);
        register(PacketType.SET_PLAYER_START, null);
        register(PacketType.FIND_UNIQUE_ENTITY_RESPONSE, null);
        register(PacketType.PONG, null);
        register(PacketType.MODIFY_TILE_LIST, null);
        register(PacketType.DAMAGE_TILE_GROUP, null);
        register(PacketType.COLLECT_LIQUID, null);
        register(PacketType.REQUEST_DROP, null);
        register(PacketType.SPAWN_ENTITY, null);
        register(PacketType.CONNECT_WIRE, null);
        register(PacketType.DISCONNECT_ALL_WIRES, null);
        register(PacketType.WORLD_CLIENT_STATE_UPDATE, null);
        register(PacketType.FIND_UNIQUE_ENTITY, null);
        register(PacketType.WORLD_START_ACKNOWLEDGE, null);
        register(PacketType.PING, null);
        register(PacketType.ENTITY_CREATE, null);
        register(PacketType.ENTITY_UPDATE, null);
        register(PacketType.ENTITY_DESTROY, null);
        register(PacketType.ENTITY_INTERACT, null);
        register(PacketType.ENTITY_INTERACT_RESULT, null);
        register(PacketType.HIT_REQUEST, null);
        register(PacketType.DAMAGE_REQUEST, null);
        register(PacketType.DAMAGE_NOTIFICATION, null);
        register(PacketType.ENTITY_MESSAGE, new EntityMessageParser());
        register(PacketType.ENTITY_MESSAGE_RESPONSE, new EntityMessageResponseParser());
        register(PacketType.UPDATE_WORLD_PROPERTIES, null);
        register(PacketType.STEP_UPDATE, new StepUpdateParser());
        register(PacketType.SYSTEM_WORLD_START, null);
        register(PacketType.SYSTEM_WORLD_UPDATE, null);
        register(PacketType.SYSTEM_OBJECT_CREATE, null);
        register(PacketType.SYSTEM_OBJECT_DESTROY, null);
        register(PacketType.SYSTEM_SHIP_CREATE, null);
        register(PacketType.SYSTEM_SHIP_DESTROY, null);
        register(PacketType.SYSTEM_OBJECT_SPAWN, null);

    }

    public <T> void register(PacketType type, PacketParser<T> parser) {
        parsers.put(type, parser);
    }

    @SuppressWarnings("unchecked")
    public <T> PacketParser<T> get(PacketType type) {
        return (PacketParser<T>) parsers.get(type);
    }
}
