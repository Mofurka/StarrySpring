package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.ban_manager.model.BanOperationResult;
import irden.space.proxy.plugin.ban_manager.persistence.BanRecordJdbcRepository;
import irden.space.proxy.plugin.ban_manager.persistence.model.BanRecord;
import irden.space.proxy.plugin.ban_manager.utils.BanFormatUtils;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;


@Component
public class BanService {

    private static final Logger log = LoggerFactory.getLogger(BanService.class);
    private static final String IP_ADDRESS_PATTERN = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b";

    private final BanRecordJdbcRepository banRecordRepository;
    private final PlayerManagerApi playerManagerApi;
    private final BanFormatUtils banFormatUtils;

    public BanService(BanRecordJdbcRepository banRecordRepository, @Lazy PlayerManagerApi playerManagerApi, BanFormatUtils banFormatUtils) {
        this.banRecordRepository = banRecordRepository;
        this.playerManagerApi = playerManagerApi;
        this.banFormatUtils = banFormatUtils;
    }

    public Optional<BanRecord> findActiveBan(String name, String playerUuid, String ipAddress) {
        BanRecord probe = BanRecord.builder()
                .name(name)
                .playerUuid(playerUuid)
                .ipAddress(ipAddress)
                .build();
        return banRecordRepository.findActiveBanByBanRecord(probe);
    }

    public boolean unban(String targetPlayer) {
        if (isIpAddress(targetPlayer)) {
            int updated = banRecordRepository.deactivateActiveBanByIpAddress(targetPlayer);
            if (updated > 0) {
                log.info("Removed active IP ban for {}", targetPlayer);
            }
            return updated > 0;
        }

        Optional<Player> optionalPlayer = playerManagerApi.findPlayer(targetPlayer, false);
        if (optionalPlayer.isEmpty()) {
            return false;
        }

        Player player = optionalPlayer.get();
        int updated = banRecordRepository.deactivateActiveBanByPlayer(player.uuid().toString(), player.name());
        if (updated > 0) {
            log.info("Removed active ban for player {} ({})", player.name(), player.uuid());
        }
        return updated > 0;
    }

    public BanOperationResult ban(String targetPlayer, String bannedBy, String durationStr, String reason) {
        boolean permanent = durationStr.equalsIgnoreCase("permanent");
        LocalDateTime expiresAt = null;

        if (!permanent) {
            try {
                expiresAt = banFormatUtils.parseExpiresAt(durationStr);
            } catch (IllegalArgumentException ex) {
                return new BanOperationResult(false, banFormatUtils.get("ban.operation.failure.invalid_duration", "1y 2m 3d 5h 6s"));
            }
        }

        if (isIpAddress(targetPlayer)) {
            return banIp(targetPlayer, bannedBy, permanent, expiresAt, reason);
        }

        return banPlayer(targetPlayer, bannedBy, permanent, expiresAt, reason);
    }

    private BanOperationResult banIp(
            String ipAddress,
            String bannedBy,
            boolean permanent,
            LocalDateTime expiresAt,
            String reason
    ) {
        BanRecord banRecord = BanRecord.builder()
                .ipAddress(ipAddress)
                .reason(reason)
                .bannedBy(bannedBy)
                .permanent(permanent)
                .bannedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        banRecordRepository.save(banRecord);
        var message = banFormatUtils.formatBanMessage(reason, permanent, expiresAt);
        playerManagerApi.findAllPlayersByIpAddress(ipAddress)
                .forEach(player -> player.kick(message));

        return new BanOperationResult(true, banFormatUtils.get("ban.operation.success.ip", ipAddress, reason));
    }



    private BanOperationResult banPlayer(
            String targetPlayer,
            String bannedBy,
            boolean permanent,
            LocalDateTime expiresAt,
            String reason
    ) {
        Optional<Player> optionalPlayer = playerManagerApi.findPlayer(targetPlayer, false);
        if (optionalPlayer.isEmpty()) {
            return new BanOperationResult(false, banFormatUtils.get("ban.operation.failure.player_not_found", targetPlayer));
        }

        Player player = optionalPlayer.get();
        BanRecord banRecord = BanRecord.builder()
                .name(player.name())
                .playerUuid(player.uuid().toString())
                .reason(reason)
                .bannedBy(bannedBy)
                .permanent(permanent)
                .bannedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        banRecordRepository.save(banRecord);
        player.kick(banFormatUtils.formatBanMessage(reason, permanent, expiresAt));

        return new BanOperationResult(true, banFormatUtils.get("ban.operation.success.player",player.name(), reason));
    }



    private boolean isIpAddress(String target) {
        return target.matches(IP_ADDRESS_PATTERN);
    }
}
