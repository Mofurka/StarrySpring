package irden.space.proxy.plugin.discord.listener;

import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import irden.space.proxy.plugin.general.events.CleanChatMessageEvent;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class DiscordChatRelayListener extends ListenerAdapter {

    private final DiscordBotConfiguration configuration;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            return;
        }

        List<Long> channelsToListen = configuration.channelsToListen();
        if (channelsToListen == null || !channelsToListen.contains(event.getChannel().getIdLong())) {
            return;
        }

        String sender = "[%s][%s] <%s>".formatted(
                event.getChannel().getName(),
                Color.ORANGE.colorString("DC"),
                event.getAuthor().getEffectiveName()
        );

        eventPublisher.publishEvent(
                new CleanChatMessageEvent(sender, "Broadcast", event.getMessage().getContentRaw())
        );
    }
}
