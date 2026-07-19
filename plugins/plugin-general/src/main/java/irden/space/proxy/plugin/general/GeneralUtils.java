package irden.space.proxy.plugin.general;

import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeneralUtils {
    private final GeneralPlugin generalPlugin;
    private final PlayerManagerApi playerManagerApi;
    private final MessageUtils messageUtils;

    @Async
    @SneakyThrows
    public void shutdownServer(String name, Integer time) {
        log.info("{} has called for a server to shutdown", name);
        if (time == null) {
            time = 5;
        }
        String code = "chat.shutdown";
        broadcastMessage(messageUtils.get(code, time));
        sleep(1_000);
        if (time != 0) {
            time = time - 1;
        }
        for (int i = time; i > 0; i--) {
            if (i < 10) {
                String message = messageUtils.get(code, i);
                broadcastMessage(message);
            } else if (i % 10 == 0) {
                String message = messageUtils.get(code, i);
                broadcastMessage(message);
            }
            sleep(1_000);
        }
        String message = messageUtils.get("chat.shutdown_notify");
        kickAll(message);
        sleep(1_000);
        System.exit(0);
    }

    public void broadcastMessage(@NotBlank String message) {
        playerManagerApi.onlinePlayers().forEach(player -> player.sendMessage(message));
    }

    public void broadcastMessage(@NotNull ChatReceive message) {
        playerManagerApi.onlinePlayers().forEach(player -> player.sendMessage(message));
    }

    public void kickAll(String message) {
        playerManagerApi.onlinePlayers().forEach(p -> p.kick(message));
    }

    public void handleWhoCommand(CommandContext ctx) {
        List<Player> players = playerManagerApi.onlinePlayers();
        ctx.sender(Player.class).ifPresent(
                player -> {
                    var invisible = !player.permissions().has(ChatPermissions.INVISIBLE_BYPASS.permission());
                    var size = players.size();
                    var message = messageUtils.get("chat.who", size);

                    if (size > 0) {
                        String collect = players.stream().map(p -> {
                                            if (!invisible || p.permissions().has(ChatPermissions.JOIN_ANNOUNCE.permission())) {
                                                return "[%s] %s".formatted(Color.RED.colorString(String.valueOf(p.clientId())), Color.colorString(p.namePrefix(), p.nickname(), true));
                                            } else return null;
                                        }
                                )
                                .collect(Collectors.joining(","));
                        message = message + System.lineSeparator() + collect;
                    }


                    player.sendMessage(message);
                }
        );
    }
}
