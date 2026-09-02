package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.discord.gateway.DiscordConnection;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public final class DiscordBotRunner implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final DiscordConnection connection;
    private final DiscordCommandRegistrar commandRegistrar;
    private final ObjectProvider<ListenerAdapter> listeners;

    @OnStart
    public void start() {
        if (!connection.start()) {
            return;
        }

        Object[] eventListeners = listeners.orderedStream().toArray();
        connection.require().addEventListener(eventListeners);
        log.info("Registered {} Discord event listeners", eventListeners.length);

        commandRegistrar.registerCommands();
    }

    @Override
    public void destroy() {
        connection.shutdown();
    }
}
