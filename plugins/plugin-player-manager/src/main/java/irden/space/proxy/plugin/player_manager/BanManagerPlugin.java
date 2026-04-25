package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
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
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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
public class BanManagerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(BanManagerPlugin.class);
    private BanRecordJdbcRepository banRecordRepository;
    private PlayerManagerPlugin playerManagerPlugin;

    @OnLoad
    public void handleLoad(PluginContext context) {
        DataSource dataSource = context.requireService(DataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.banRecordRepository = new BanRecordJdbcRepository(jdbcTemplate);
        this.playerManagerPlugin = context.requireService(PlayerManagerPlugin.class);
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
                    "This ban expires at " + activeBanRecord.expiresAt().toString() + "[UTC]" + " (in " + java.time.Duration.between(LocalDateTime.now(), activeBanRecord.expiresAt()).toMinutes() + " minutes)";
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
//        if (context.arguments().isEmpty()) {
//            context.reply("Usage: /ban <IP, ID, UUID, NAME> [duration] [reason]");
//            return;
//        }
//
//        List<String> arguments = context.arguments();
//        String targetPlayer = arguments.get(0);
//        String durationStr = arguments.size() > 1 ? arguments.get(1) : "permanent";
//        String reason = arguments.size() > 2
//                ? String.join(" ", arguments.subList(2, arguments.size()))
//                : "No reason provided";
//
//        Player player = playerManagerPlugin.getPlayerBySessionId(context.session().sessionId()).orElse(null);
//        BanOperationResult result = ban(targetPlayer, player == null ? null : player.name(), durationStr, reason);
//        context.reply(result.message());
        return literal("ban")
                .then(argument("target", StringArgumentType.word())
                        .then(argument("duration", StringArgumentType.word())
                                .optional()
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .optional()
                                        .executes(context -> {
                                            String target = context.get("target", String.class);
                                            String duration = context.getOrDefault("duration", String.class, "permanent");
                                            String reason = context.getOrDefault("reason", String.class, "No reason provided");

                                            Player player = playerManagerPlugin.getPlayerBySessionId(context.session().sessionId()).orElse(null);
                                            BanOperationResult result = ban(target, player == null ? null : player.name(), duration, reason);
                                            context.reply(result.message());
                                        }))))
                .build();
    }

    @ChatCommand(
            value = "kick",
            description = "Kick a player from the server."
    )
    public CommandSpec kickCommand() {
        return literal("kick")
                .then(argument("target", StringArgumentType.word())
                        .then(argument("reason", StringArgumentType.greedyString())
                                .optional()
                                .executes(context -> {
                                    String target = context.get("target", String.class);
                                    String reason = context.getOrDefault("reason", String.class, "No reason");
                                    context.reply("Kicked " + target + ". Reason: " + reason);
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
                .then(argument("target", StringArgumentType.word())
                        .executes(context -> {
                            String target = context.get("target", String.class);
                            boolean success = unban(target);
                            if (success) {
                                context.reply("Successfully unbanned " + target);
                            } else {
                                context.reply("No active ban found for " + target);
                            }
                        }))
                .build();
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

        Optional<Player> optionalPlayer = playerManagerPlugin.findPlayer(targetPlayer, false);
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
        playerManagerPlugin.findAllPlayersByIpAddress(ipAddress)
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
        Optional<Player> optionalPlayer = playerManagerPlugin.findPlayer(targetPlayer, false);
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

