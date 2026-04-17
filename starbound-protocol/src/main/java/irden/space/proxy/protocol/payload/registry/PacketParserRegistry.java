package irden.space.proxy.protocol.payload.registry;

import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatRecieveParser;
import irden.space.proxy.protocol.payload.packet.chat.ChatSentParser;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnectParser;
import irden.space.proxy.protocol.payload.packet.client_disconnect_request.ClientDisconnectRequestParser;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailureParser;
import irden.space.proxy.protocol.payload.packet.connect.ConnectSuccessParser;
import irden.space.proxy.protocol.payload.packet.damage.RemoteDamageNotificationParser;
import irden.space.proxy.protocol.payload.packet.damage.RemoteDamageRequestParser;
import irden.space.proxy.protocol.payload.packet.entity.create.EntityCreateParser;
import irden.space.proxy.protocol.payload.packet.entity.update.EntityUpdateParser;
import irden.space.proxy.protocol.payload.packet.entity_interact.EntityInteractParser;
import irden.space.proxy.protocol.payload.packet.entity_interact.EntityInteractResultParser;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessageParser;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageResponseParser;
import irden.space.proxy.protocol.payload.packet.fly_ship.FlyShipParser;
import irden.space.proxy.protocol.payload.packet.handshake.HandshakeChallengeParser;
import irden.space.proxy.protocol.payload.packet.handshake.HandshakeResponseParser;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.ModifyTileListCodec;
import irden.space.proxy.protocol.payload.packet.pause.PauseParser;
import irden.space.proxy.protocol.payload.packet.ping_pong.PingParser;
import irden.space.proxy.protocol.payload.packet.ping_pong.PongParser;
import irden.space.proxy.protocol.payload.packet.protocol_request.ProtocolRequestParser;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponseParser;
import irden.space.proxy.protocol.payload.packet.server_disconnect.ServerDisconnectParser;
import irden.space.proxy.protocol.payload.packet.step_update.StepUpdateParser;
import irden.space.proxy.protocol.payload.packet.universe_time_update.UniverseTimeUpdateParser;
import irden.space.proxy.protocol.payload.packet.warp.player_warp.PlayerWarpParser;
import irden.space.proxy.protocol.payload.packet.warp.player_warp_result.PlayerWarpResultParser;
import irden.space.proxy.protocol.payload.packet.world_start.WorldStartParser;
import irden.space.proxy.protocol.payload.packet.world_stop.WorldStopParser;

import java.util.EnumMap;
import java.util.Map;

public class PacketParserRegistry {

    private final Map<PacketType, PacketParser<?>> parsers = new EnumMap<>(PacketType.class);

    public PacketParserRegistry() {
        register(PacketType.PROTOCOL_REQUEST, new ProtocolRequestParser());
        register(PacketType.PROTOCOL_RESPONSE, new ProtocolResponseParser());

        // universe server -> universe client
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

        // univere client -> universe server
        register(PacketType.CLIENT_CONNECT, new ClientConnectParser());
        register(PacketType.CLIENT_DISCONNECT_REQUEST, new ClientDisconnectRequestParser());
        register(PacketType.HANDSHAKE_RESPONSE, new HandshakeResponseParser());
        register(PacketType.PLAYER_WARP, new PlayerWarpParser());
        register(PacketType.FLY_SHIP, new FlyShipParser());
        register(PacketType.CHAT_SENT, new ChatSentParser());
        register(PacketType.CELESTIAL_REQUEST, null);

        // universe client <-> universe server
        register(PacketType.CLIENT_CONTEXT_UPDATE, null);

        //world server -> world client
        register(PacketType.WORLD_START, new WorldStartParser());
        register(PacketType.WORLD_STOP, new WorldStopParser());
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
        register(PacketType.PONG, new PongParser());

        // world client -> world server
        register(PacketType.MODIFY_TILE_LIST, new ModifyTileListCodec());
        register(PacketType.DAMAGE_TILE_GROUP, null);
        register(PacketType.COLLECT_LIQUID, null);
        register(PacketType.REQUEST_DROP, null);
        register(PacketType.SPAWN_ENTITY, null);
        register(PacketType.CONNECT_WIRE, null);
        register(PacketType.DISCONNECT_ALL_WIRES, null);
        register(PacketType.WORLD_CLIENT_STATE_UPDATE, null);
        register(PacketType.FIND_UNIQUE_ENTITY, null);
        register(PacketType.WORLD_START_ACKNOWLEDGE, null);
        register(PacketType.PING, new PingParser());

        // system server <-> client
        register(PacketType.ENTITY_CREATE, new EntityCreateParser());
        register(PacketType.ENTITY_UPDATE, new EntityUpdateParser());
        register(PacketType.ENTITY_DESTROY, null);
        register(PacketType.ENTITY_INTERACT, new EntityInteractParser());
        register(PacketType.ENTITY_INTERACT_RESULT, new EntityInteractResultParser());
        register(PacketType.HIT_REQUEST, null);
        register(PacketType.REMOTE_DAMAGE_REQUEST, new RemoteDamageRequestParser());
        register(PacketType.REMOTE_DAMAGE_NOTIFICATION, new RemoteDamageNotificationParser());
        register(PacketType.ENTITY_MESSAGE, new EntityMessageParser());
        register(PacketType.ENTITY_MESSAGE_RESPONSE, new EntityMessageResponseParser());
        register(PacketType.UPDATE_WORLD_PROPERTIES, null);
        register(PacketType.STEP_UPDATE, new StepUpdateParser());

        // system server -> system client
        register(PacketType.SYSTEM_WORLD_START, null);
        register(PacketType.SYSTEM_WORLD_UPDATE, null);
        register(PacketType.SYSTEM_OBJECT_CREATE, null);
        register(PacketType.SYSTEM_OBJECT_DESTROY, null);
        register(PacketType.SYSTEM_SHIP_CREATE, null);
        register(PacketType.SYSTEM_SHIP_DESTROY, null);

        // client -> system server
        register(PacketType.SYSTEM_OBJECT_SPAWN, null);

        // OpenStarbound packets
        // client -> server
        register(PacketType.REPALCE_TILE_LIST, null);
        register(PacketType.UPDATE_WORLD_TEMPLATE, null);

    }

    public <T> void register(PacketType type, PacketParser<T> parser) {
        parsers.put(type, parser);
    }

    @SuppressWarnings("unchecked")
    public <T> PacketParser<T> get(PacketType type) {
        return (PacketParser<T>) parsers.get(type);
    }
}
