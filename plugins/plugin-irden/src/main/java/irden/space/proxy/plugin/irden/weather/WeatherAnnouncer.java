package irden.space.proxy.plugin.irden.weather;

import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.discord.api.DiscordEmbed;
import irden.space.proxy.plugin.discord.api.DiscordGateway;
import irden.space.proxy.plugin.discord.api.DiscordMessage;
import irden.space.proxy.plugin.discord.api.DiscordMessageRef;
import irden.space.proxy.plugin.irden.IrdenConfig;
import irden.space.proxy.plugin.native_server_lifespan.rcon.StarboundRconClient;
import irden.space.proxy.plugin.player_manager.events.PlayerConnectedEvent;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
class WeatherAnnouncer {
    private final DiscordGateway discord;
    private final IrdenConfig irdenConfig;
    private final WeatherService weatherService;
    private final ObjectProvider<StarboundRconClient> starboundRconProvider;
    private volatile DiscordMessageRef panel;

    @EventListener
    @Async
    public void onWeatherUpdated(WeatherUpdatedEvent event) {
        Long channelId = irdenConfig.weather().announceDiscordChannelId();
        int i = Integer.parseInt(event.snapshot().color().substring(2), 16);
        String s = event.snapshot().richDescription();

        var embed = DiscordEmbed.builder()
                .title("Погода")
                .description(s)
                .imageUrl(event.snapshot().image())
                .color(i)
                .build();
        discord.whenReady().thenRun(() ->
                {
                    if (panel == null) {
                        try {
                            discord.deleteLastMessage(channelId).thenRun(() -> {
                            }).get();
                        } catch (InterruptedException | ExecutionException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                    discord.setChannelName(channelId, event.snapshot().channelName());
                    discord.publish(panel, channelId, DiscordMessage.of(
                                    embed
                            )
                    ).thenAccept(ref -> panel = ref);

                }
        );
    }

    @EventListener
    @Async
    public void onPlayerConnected(PlayerConnectedEvent event) {
        Player player = event.player();
        WeatherSnapshot weatherSnapshot = weatherService.currentSnapshot();
        var description = weatherSnapshot.plainDescription();
        ChatReceive chatReceive = ChatReceive.builder()
                .header(
                        ChatHeader.builder()
                                .mode(ChatReceiveMode.COMMAND_RESULT)
                                .clientId(0)
                                .build()
                )
                .name(Color.ORANGE.colorString("Погода"))
                .message(Color.ORANGE.colorString("Погода") + ": " + description)
                .build();
        player.sendMessage(chatReceive);
        starboundRconProvider.ifAvailable(starboundRcon -> starboundRcon.executeUnchecked("setweather %s force -678786925:457929228:-22722436:9".formatted(weatherSnapshot.serverWeather())));
    }

}
