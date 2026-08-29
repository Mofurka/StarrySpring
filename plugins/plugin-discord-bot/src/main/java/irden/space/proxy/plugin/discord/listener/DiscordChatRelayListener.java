package irden.space.proxy.plugin.discord.listener;

import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.discord.config.ChannelToListen;
import irden.space.proxy.plugin.discord.config.DiscordBotConfiguration;
import irden.space.proxy.plugin.discord.gateway.DiscordConnection;
import irden.space.proxy.plugin.general.events.CleanChatMessageEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class DiscordChatRelayListener extends ListenerAdapter {

    private final List<ChannelToListen> channelToListen;
    private final ApplicationEventPublisher eventPublisher;
    private final DiscordConnection connection;

    public DiscordChatRelayListener(DiscordBotConfiguration configuration, ApplicationEventPublisher eventPublisher, DiscordConnection connection) {
        this.channelToListen = configuration.channelsToListen();
        this.eventPublisher = eventPublisher;
        this.connection = connection;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Optional<JDA> jda = connection.jda();
        if (jda.isPresent() && jda.get().getSelfUser().equals(event.getAuthor()) || event.getAuthor().isSystem()) {
            return;
        }

        var channel = channelToListen.stream()
                .filter(c -> c.channelId() == event.getChannel().getIdLong())
                .findFirst()
                .orElse(null);
        if (channel == null) {
            return;
        }
        StringBuilder senderBuilder = new StringBuilder();
        if (channel.nameOverride() != null) {
            senderBuilder.append("[").append(channel.nameOverride()).append("]");
        }
        if (channel.modeOverride() != null) {
            senderBuilder.append("[").append(Color.ORANGE.colorString(channel.modeOverride())).append("]");
        } else {
            senderBuilder.append("[").append(Color.ORANGE.colorString("DC")).append("]");
        }
        if (channel.authorNameOverride() == null) {
            senderBuilder.append(" <").append(event.getAuthor().getEffectiveName()).append(">");
        } else if (!channel.authorNameOverride().equals("noop")){
            senderBuilder.append(" <").append(channel.authorNameOverride()).append(">");
        }
        String sender = senderBuilder.toString();

        String mode = channel.modeOverride() != null ? channel.modeOverride() : "Broadcast";

        eventPublisher.publishEvent(
                new CleanChatMessageEvent(sender, mode, event.getMessage().getContentRaw())
        );
    }
}
