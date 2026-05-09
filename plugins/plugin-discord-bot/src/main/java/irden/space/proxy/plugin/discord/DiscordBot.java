package irden.space.proxy.plugin.discord;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public final class DiscordBot {
    private final Logger log = LoggerFactory.getLogger(DiscordBot.class);
    private final JDA jda;

    public DiscordBot(String token) {
        EnumSet<GatewayIntent> intents = EnumSet.allOf(GatewayIntent.class);
        this.jda = JDABuilder.createLight(token, intents)
                .addEventListeners(new TestListener())
                .build();
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }


    public static class TestListener extends ListenerAdapter {
        private final Logger log = LoggerFactory.getLogger(TestListener.class);

        @Override
        public void onReady(@NotNull ReadyEvent event) {
            log.info("Discord bot is ready! Logged in as: {}", event.getJDA().getSelfUser().getAsTag());
        }

        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
//            if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
//                return;
//            }
//            log.info("Received message from {}: {}", event.getAuthor().getAsTag(), event.getMessage().getChannel());
        }


    }
}
