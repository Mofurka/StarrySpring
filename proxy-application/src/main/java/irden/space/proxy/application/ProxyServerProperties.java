package irden.space.proxy.application;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proxy")
public class ProxyServerProperties {

    private String listenHost = "0.0.0.0";
    private int listenPort = 21025;

    private String upstreamHost = "127.0.0.1";
    private int upstreamPort = 21026;

    public String getListenHost() {
        return listenHost;
    }

    public void setListenHost(String listenHost) {
        this.listenHost = listenHost;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getUpstreamHost() {
        return upstreamHost;
    }

    public void setUpstreamHost(String upstreamHost) {
        this.upstreamHost = upstreamHost;
    }

    public int getUpstreamPort() {
        return upstreamPort;
    }

    public void setUpstreamPort(int upstreamPort) {
        this.upstreamPort = upstreamPort;
    }

    @Override
    public String toString() {
        return "ProxyServerProperties{" +
                "listenHost='" + listenHost + '\'' +
                ", listenPort=" + listenPort +
                ", upstreamHost='" + upstreamHost + '\'' +
                ", upstreamPort=" + upstreamPort +
                '}';
    }
}
