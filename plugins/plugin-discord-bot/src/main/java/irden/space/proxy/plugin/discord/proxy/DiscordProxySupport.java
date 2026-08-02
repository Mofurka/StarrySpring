package irden.space.proxy.plugin.discord.proxy;

import com.neovisionaries.ws.client.WebSocketFactory;
import net.dv8tion.jda.api.JDABuilder;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;
import java.time.Duration;
import java.util.Objects;


public final class DiscordProxySupport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);

    private DiscordProxySupport() {
    }

    public static void apply(JDABuilder builder, Socks5Proxy proxy) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(proxy, "proxy");

        builder.setHttpClientBuilder(httpClientBuilder(proxy));
        builder.setWebsocketFactory(websocketFactory(proxy));
    }

    private static OkHttpClient.Builder httpClientBuilder(Socks5Proxy proxy) {
        return new OkHttpClient.Builder()
                .socketFactory(new Socks5SocketFactory(proxy))
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .writeTimeout(WRITE_TIMEOUT);
    }

    private static WebSocketFactory websocketFactory(Socks5Proxy proxy) {
        SSLSocketFactory defaultSslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        return new WebSocketFactory()
                .setSSLSocketFactory(new Socks5SslSocketFactory(defaultSslSocketFactory, proxy))
                .setConnectionTimeout((int) CONNECT_TIMEOUT.toMillis())
                .setVerifyHostname(true);
    }
}
