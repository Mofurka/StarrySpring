package irden.space.proxy.plugin.discord.proxy;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

final class Socks5Socket extends Socket {

    private static final byte VERSION = 0x05;
    private static final byte AUTH_VERSION = 0x01;
    private static final byte AUTH_NONE = 0x00;
    private static final byte AUTH_USERNAME_PASSWORD = 0x02;
    private static final byte AUTH_UNSUPPORTED = (byte) 0xFF;
    private static final byte COMMAND_CONNECT = 0x01;
    private static final byte ADDRESS_IPV4 = 0x01;
    private static final byte ADDRESS_DOMAIN = 0x03;
    private static final byte ADDRESS_IPV6 = 0x04;

    private final Socks5Proxy proxy;
    private final SSLSocketFactory tlsFactory;

    private volatile Socket tlsSocket;

    Socks5Socket(Socks5Proxy proxy, SSLSocketFactory tlsFactory) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
        this.tlsFactory = tlsFactory;
    }

    private static boolean isAddressLiteral(InetSocketAddress target) {
        InetAddress address = target.getAddress();
        return address != null && address.getHostAddress().equals(target.getHostString());
    }

    private static byte[] readFully(InputStream input, int length) throws IOException {
        byte[] buffer = input.readNBytes(length);
        if (buffer.length != length) {
            throw new EOFException("Proxy closed the connection during SOCKS5 handshake");
        }
        return buffer;
    }

    private static String replyMessage(byte reply) {
        return switch (reply) {
            case 0x01 -> "general failure";
            case 0x02 -> "connection not allowed by ruleset";
            case 0x03 -> "network unreachable";
            case 0x04 -> "host unreachable";
            case 0x05 -> "connection refused";
            case 0x06 -> "ttl expired";
            case 0x07 -> "command not supported";
            case 0x08 -> "address type not supported";
            default -> "code " + (reply & 0xFF);
        };
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (!(endpoint instanceof InetSocketAddress target)) {
            throw new IOException("Unsupported endpoint type: " + endpoint);
        }

        super.connect(new InetSocketAddress(proxy.host(), proxy.port()), timeout);

        int previousTimeout = getSoTimeout();
        if (timeout > 0) {
            setSoTimeout(timeout);
        }

        try {
            InputStream input = super.getInputStream();
            OutputStream output = super.getOutputStream();

            authenticate(input, output);
            requestTunnel(input, output, target);

            if (tlsFactory != null) {
                tlsSocket = startTls(target);
            }
        } finally {
            restoreTimeout(previousTimeout);
        }
    }

    private void restoreTimeout(int previousTimeout) {
        try {
            if (!isClosed()) {
                setSoTimeout(previousTimeout);
            }
        } catch (IOException _) {
            // сокет уже нерабочий, исходное исключение важнее
        }
    }

    private void authenticate(InputStream input, OutputStream output) throws IOException {
        boolean credentials = proxy.hasCredentials();

        output.write(credentials
                ? new byte[]{VERSION, 2, AUTH_NONE, AUTH_USERNAME_PASSWORD}
                : new byte[]{VERSION, 1, AUTH_NONE});
        output.flush();

        byte[] choice = readFully(input, 2);
        if (choice[0] != VERSION) {
            throw new IOException("Proxy answered with unexpected SOCKS version: " + (choice[0] & 0xFF));
        }

        byte method = choice[1];
        if (method == AUTH_NONE) {
            return;
        }
        if (method == AUTH_UNSUPPORTED) {
            throw new IOException("Proxy rejected all offered authentication methods");
        }
        if (method != AUTH_USERNAME_PASSWORD) {
            throw new IOException("Proxy requested unsupported authentication method: " + (method & 0xFF));
        }
        if (!credentials) {
            throw new IOException("Proxy requires username/password, but none is configured");
        }

        byte[] username = proxy.username().getBytes(StandardCharsets.UTF_8);
        byte[] password = (proxy.password() == null ? "" : proxy.password()).getBytes(StandardCharsets.UTF_8);
        if (username.length > 255 || password.length > 255) {
            throw new IOException("Proxy credentials are too long for SOCKS5");
        }

        ByteArrayOutputStream request = new ByteArrayOutputStream(3 + username.length + password.length);
        request.write(AUTH_VERSION);
        request.write(username.length);
        request.writeBytes(username);
        request.write(password.length);
        request.writeBytes(password);

        output.write(request.toByteArray());
        output.flush();

        byte[] result = readFully(input, 2);
        if (result[1] != 0) {
            throw new IOException("Proxy rejected credentials, status: " + (result[1] & 0xFF));
        }
    }

    private void requestTunnel(InputStream input, OutputStream output, InetSocketAddress target) throws IOException {
        String host = target.getHostString();
        int port = target.getPort();

        ByteArrayOutputStream request = new ByteArrayOutputStream();
        request.write(VERSION);
        request.write(COMMAND_CONNECT);
        request.write(0x00);

        if (isAddressLiteral(target)) {
            byte[] address = target.getAddress().getAddress();
            request.write(address.length == 4 ? ADDRESS_IPV4 : ADDRESS_IPV6);
            request.writeBytes(address);
        } else {
            // Имя отдаём прокси как есть: DNS резолвится на его стороне.
            byte[] domain = host.getBytes(StandardCharsets.US_ASCII);
            if (domain.length == 0 || domain.length > 255) {
                throw new IOException("Hostname does not fit into a SOCKS5 request: " + host);
            }
            request.write(ADDRESS_DOMAIN);
            request.write(domain.length);
            request.writeBytes(domain);
        }

        request.write((port >> 8) & 0xFF);
        request.write(port & 0xFF);

        output.write(request.toByteArray());
        output.flush();

        byte[] reply = readFully(input, 4);
        if (reply[0] != VERSION) {
            throw new IOException("Proxy answered with unexpected SOCKS version: " + (reply[0] & 0xFF));
        }
        if (reply[1] != 0) {
            throw new IOException(
                    "Proxy refused to connect to " + host + ":" + port + " — " + replyMessage(reply[1])
            );
        }

        skipBoundAddress(input, reply[3]);
    }

    private void skipBoundAddress(InputStream input, byte addressType) throws IOException {
        switch (addressType) {
            case ADDRESS_IPV4 -> readFully(input, 4 + 2);
            case ADDRESS_IPV6 -> readFully(input, 16 + 2);
            case ADDRESS_DOMAIN -> {
                int length = readFully(input, 1)[0] & 0xFF;
                readFully(input, length + 2);
            }
            default -> throw new IOException("Unknown address type in proxy reply: " + (addressType & 0xFF));
        }
    }

    private Socket startTls(InetSocketAddress target) throws IOException {
        String host = target.getHostString();

        SSLSocket ssl = (SSLSocket) tlsFactory.createSocket(this, host, target.getPort(), false);

        SSLParameters parameters = ssl.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        if (!isAddressLiteral(target)) {
            parameters.setServerNames(List.of(new SNIHostName(host)));
        }
        ssl.setSSLParameters(parameters);

        ssl.startHandshake();

        return ssl;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Socket tls = tlsSocket;
        return tls == null ? super.getInputStream() : tls.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        Socket tls = tlsSocket;
        return tls == null ? super.getOutputStream() : tls.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;

        Socket tls = tlsSocket;
        if (tls != null) {
            try {
                tls.close();
            } catch (IOException e) {
                failure = e;
            }
        }

        try {
            super.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
