package irden.space.proxy.plugin.discord.proxy;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;


public final class Socks5SslSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;
    private final Socks5Proxy proxy;

    public Socks5SslSocketFactory(SSLSocketFactory delegate, Socks5Proxy proxy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.proxy = Objects.requireNonNull(proxy, "proxy");
    }

    @Override
    public Socket createSocket() {
        return new Socks5Socket(proxy, delegate);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(InetSocketAddress.createUnresolved(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(InetSocketAddress.createUnresolved(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port));
        return socket;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(socket, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
