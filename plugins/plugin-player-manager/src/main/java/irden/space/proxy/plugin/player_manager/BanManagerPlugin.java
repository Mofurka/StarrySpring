package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.BanTarget;
import irden.space.proxy.plugin.player_manager.command.BanTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.BanOperationResult;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.BanRecordJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.BanRecord;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;

@PluginDefinition(
        id = "ban-manager",
        name = "Ban Manager",
        version = "1.0.0",
        description = "An advanced ban system that allows banning players by name, UUID, or IP address, with support for temporary and permanent bans.",
        author = "https://github.com/Mofurka",
        dependsOn = {"player-manager"}
)
@PluginSpringConfiguration(value = BanManagerSpringConfiguration.class, scanPluginPackage = false)
@Component
public class BanManagerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(BanManagerPlugin.class);
    private final BanRecordJdbcRepository banRecordRepository;
    private final PlayerManagerApi playerManagerApi;

    public BanManagerPlugin(BanRecordJdbcRepository banRecordRepository, PlayerManagerApi playerManagerApi) {
        this.banRecordRepository = banRecordRepository;
        this.playerManagerApi = playerManagerApi;
    }


    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext context) {
        ClientConnect clientConnect = (ClientConnect) context.parsedPayload();
        BanRecord banRecord = BanRecord.builder()
                .name(clientConnect.playerName())
                .playerUuid(clientConnect.playerUuid().toString())
                .ipAddress(context.session().clientIp())
                .build();
        Optional<BanRecord> optionalBanRecord = banRecordRepository.findActiveBanByBanRecord(banRecord);
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

    @ChatCommand(
            value = "ban",
            description = "Ban a player from the server.",
            usage = "/ban <player> [duration] [reason]"
    )
    public CommandSpec handleBanCommand() {
        return literal("ban")
                .then(argument("target", BanTargetArgumentType.banTarget(() -> playerManagerApi))
                        .then(argument("duration", StringArgumentType.word())
                                .optional()
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .optional()
                                        .executes(this::handleBan))))
                .build();
    }

    @ChatCommand(
            value = "kick",
            description = "Kick a player from the server."
    )
    public CommandSpec kickCommand() {
        return literal("kick")
                .then(argument("target", PlayerOnlineTargetArgumentType.playerTarget(()  -> playerManagerApi))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .optional()
                                .executes(context -> {
                                    PlayerTarget target = context.get("target", PlayerTarget.class);
                                    String reason = context.getOrDefault("reason", String.class, "No reason");
                                    target.player().kick(reason);
                                    context.reply("Kicked " + target.player().name() + ". Reason: " + reason);
                                })))
                .build();
    }


    @ChatCommand(
            value = "unban",
            description = "Unban a player from the server.",
            usage = "/unban <player>"
    )
    public CommandSpec handleUnbanCommand() {
        return literal("unban")
                .then(argument("target", BanTargetArgumentType.banTarget(() -> playerManagerApi))
                        .executes(context -> {
                            BanTarget target = context.get("target", BanTarget.class);
                            boolean success = unban(target.value());
                            if (success) {
                                context.reply("Successfully unbanned " + target.value());
                            } else {
                                context.reply("No active ban found for " + target.value());
                            }
                        }))
                .build();
    }

    private void handleBan(CommandContext context) {
        BanTarget target = context.get("target", BanTarget.class);
        Player executor = context.sender(Player.class).orElse(Player.builder().name("Unknown").build());

        BanOperationResult result = ban(
                target.value(),
                executor == null ? null : executor.name(),
                context.getOrDefault("duration", String.class, "permanent"),
                context.getOrDefault("reason", String.class, "No reason provided")
        );

        context.reply(result.message());
    }

    public boolean unban(String targetPlayer) {
        boolean isIp = targetPlayer.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        if (isIp) {
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
                expiresAt = parseExpiresAt(durationStr);
            } catch (IllegalArgumentException ex) {
                return new BanOperationResult(false, "Неверный формат длительности. Пример: 5d 30m");
            }
        }

        boolean isIp = targetPlayer.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        if (isIp) {
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
        playerManagerApi.findAllPlayersByIpAddress(ipAddress)
                .forEach(player -> player.kick("You have been banned from this server. \nReason: " + reason));

        return new BanOperationResult(true, "IP address " + ipAddress + " has been banned. Reason: " + reason);
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
            return new BanOperationResult(false, "Player not found: " + targetPlayer);
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
        player.kick("You have been banned from this server. Reason: " + reason);

        return new BanOperationResult(true, "Player " + player.name() + " has been banned. Reason: " + reason);
    }


    private LocalDateTime parseExpiresAt(String durationStr) {
        LocalDateTime expiresAt = LocalDateTime.now();

        for (String part : durationStr.trim().split("\\s+")) {
            if (part.length() < 2) {
                throw new IllegalArgumentException("Invalid duration part: " + part);
            }

            long value;
            try {
                value = Long.parseLong(part.substring(0, part.length() - 1));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid duration value: " + part, ex);
            }

            char unit = Character.toLowerCase(part.charAt(part.length() - 1));
            switch (unit) {
                case 's' -> expiresAt = expiresAt.plusSeconds(value);
                case 'm' -> expiresAt = expiresAt.plusMinutes(value);
                case 'h' -> expiresAt = expiresAt.plusHours(value);
                case 'd' -> expiresAt = expiresAt.plusDays(value);
                case 'w' -> expiresAt = expiresAt.plusWeeks(value);
                default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }

        return expiresAt;
    }
}

