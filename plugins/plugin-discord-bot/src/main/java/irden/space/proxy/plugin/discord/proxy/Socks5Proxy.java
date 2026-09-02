package irden.space.proxy.plugin.discord.proxy;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;


public record Socks5Proxy(String host, int port, String username, String password) {

    private static final int DEFAULT_PORT = 1080;
    private static final Set<String> SUPPORTED_SCHEMES = Set.of("socks", "socks5", "socks5h");

    public Socks5Proxy {
        Objects.requireNonNull(host, "host");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Proxy port is out of range: " + port);
        }
    }

    public static Socks5Proxy parse(String proxyUrl) {
        Objects.requireNonNull(proxyUrl, "proxyUrl");

        String value = proxyUrl.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Proxy url is empty");
        }

        URI uri = URI.create(value.contains("://") ? value : "socks5://" + value);

        String scheme = uri.getScheme() == null
                ? "socks5"
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Only socks5 proxies are supported, got scheme: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Proxy host is missing in url");
        }

        int port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();

        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return new Socks5Proxy(host, port, null, null);
        }

        int separator = userInfo.indexOf(':');
        String username = decode(separator < 0 ? userInfo : userInfo.substring(0, separator));
        String password = separator < 0 ? "" : decode(userInfo.substring(separator + 1));

        return new Socks5Proxy(host, port, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    public boolean hasCredentials() {
        return username != null && !username.isEmpty();
    }

    @Override
    public String toString() {
        return "socks5://" + (hasCredentials() ? username + ":***@" : "") + host + ":" + port;
    }
}
