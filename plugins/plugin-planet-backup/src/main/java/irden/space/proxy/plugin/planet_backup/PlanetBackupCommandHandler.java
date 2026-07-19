package irden.space.proxy.plugin.planet_backup;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.general.GeneralUtils;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import irden.space.proxy.protocol.payload.packet.server_disconnect.ServerDisconnect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanetBackupCommandHandler {
    private final PlanetBackupService backupService;
    private final GeneralUtils generalUtils;
    private final AtomicBoolean atomicClose = new AtomicBoolean(false);

    public void startDelayedBackup() {
        Thread.ofVirtual().name("planet-backup-manual").start(() -> {
            atomicClose.set(true);
            try {
                var time = 30;
                generalUtils.broadcastMessage("Server is starting planet backup schedule! 30 seconds until server shutdown.");
                sleep(1_000);
                for (int i = time; i > 0; i--) {
                    if (i < 10) {
                        generalUtils.broadcastMessage("Shutdown in %s seconds".formatted(i));
                    } else if (i % 10 == 0) {
                        generalUtils.broadcastMessage("Shutdown in %s seconds".formatted(i));
                    }
                    sleep(1_000);
                }
                String message = "Server shutdown. Planet backup system initiated.";
                generalUtils.kickAll(message);
                sleep(3_000);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                log.info("Planet backup thread is interrupted. WHY?!");
            }
            try {
                Path archive = backupService.backupNow();
                log.info("Manual planet backup finished: {}", archive);
            } catch (Exception e) {
                log.error("Manual planet backup failed", e);
            }
            atomicClose.set(false);
        });
    }

    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext ctx) {
        if (atomicClose.get()) {
            String message = "Server is closed for planet backup. Try again in 1 minute.";
            ConnectFailure connectFailure = new ConnectFailure(message);
            return PacketDecision.cancel(() -> ctx.session().sendToClient(PacketType.CONNECT_FAILURE, connectFailure));
        }
        return PacketDecision.forward();
    }
}
