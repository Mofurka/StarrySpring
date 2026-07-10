package irden.space.proxy.plugin.ban_manager;

import irden.space.proxy.plugin.ban_manager.command.BanTarget;
import irden.space.proxy.plugin.ban_manager.command.BanTargetArgumentType;
import irden.space.proxy.plugin.ban_manager.model.BanOperationResult;
import irden.space.proxy.plugin.ban_manager.utils.BanFormatUtils;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;


@Component
@RequiredArgsConstructor
public class BanCommands {

    private final BanService banService;
    private final PlayerManagerApi playerManagerApi;
    private final BanFormatUtils banFormatUtils;

    @ChatCommand(
            value = "ban",
            description = "Ban a player from the server."
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
                .then(argument("target", PlayerOnlineTargetArgumentType.playerTarget(() -> playerManagerApi))
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
            description = "Unban a player from the server."
    )
    public CommandSpec handleUnbanCommand() {
        return literal("unban")
                .then(argument("target", BanTargetArgumentType.banTarget(() -> playerManagerApi))
                        .executes(context -> {
                            BanTarget target = context.get("target", BanTarget.class);
                            boolean success = banService.unban(target.value());
                            if (success) {
                                context.reply(banFormatUtils.get("ban.message.unban.success", target.value()));
                            } else {
                                context.reply(banFormatUtils.get("ban.message.unban.not_found", target.value()));
                            }
                        }))
                .build();
    }

    private void handleBan(CommandContext context) {
        BanTarget target = context.get("target", BanTarget.class);
        Player executor = context.sender(Player.class).orElse(Player.builder().name("Unknown").build());

        BanOperationResult result = banService.ban(
                target.value(),
                executor == null ? null : executor.name(),
                context.getOrDefault("duration", String.class, "permanent"),
                context.getOrDefault("reason", String.class, banFormatUtils.get("ban.operation.default_reason"))
        );

        context.reply(result.message());
    }
}
