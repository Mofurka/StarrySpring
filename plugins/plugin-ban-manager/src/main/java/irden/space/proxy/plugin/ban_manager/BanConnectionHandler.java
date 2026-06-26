package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.ban_manager.persistence.model.BanRecord;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class BanConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(BanConnectionHandler.class);

    private final BanService banService;

    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext context) {
        ClientConnect clientConnect = (ClientConnect) context.parsedPayload();
        Optional<BanRecord> optionalBanRecord = banService.findActiveBan(
                clientConnect.playerName(),
                clientConnect.playerUuid().toString(),
                context.session().clientIp()
        );
        if (optionalBanRecord.isPresent()) {
            BanRecord activeBanRecord = optionalBanRecord.get();
            String reason = activeBanRecord.reason() != null ? activeBanRecord.reason() : "No reason provided";
            String expiresInfo = activeBanRecord.permanent() ? "This ban is permanent." :
                    "This ban expires at " + activeBanRecord.expiresAt().toString() + "[UTC]" + " (in " + Duration.between(LocalDateTime.now(), activeBanRecord.expiresAt()).toMinutes() + " minutes)";
            String message = "You are banned from this server. \nReason: " + reason + "\n" + expiresInfo;

            log.info("Blocked connection from {} (UUID: {}) due to active ban. Reason: {}",
                    clientConnect.playerName(), clientConnect.playerUuid(), reason);
            sendRejection(context.session(), message);
            return PacketDecision.cancel();
        }
        return PacketDecision.forward();
    }

    private void sendRejection(PluginSessionContext session, String message) {
        log.info("Sending rejection message to {}: {}", session.clientIp(), message);
        ConnectFailure connectFailure = new ConnectFailure(message);
        session.sendToClient(PacketType.CONNECT_FAILURE, connectFailure);
    }
}
