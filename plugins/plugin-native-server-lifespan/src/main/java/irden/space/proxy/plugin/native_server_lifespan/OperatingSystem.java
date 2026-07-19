package irden.space.proxy.plugin.native_server_lifespan;

public enum OperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    OTHER;

    public static OperatingSystem current() {
        String osName = System.getProperty("os.name", "")
                .toLowerCase();

        if (osName.contains("win")) {
            return WINDOWS;
        }

        if (osName.contains("linux")) {
            return LINUX;
        }

        if (osName.contains("mac") || osName.contains("darwin")) {
            return MACOS;
        }

        return OTHER;
    }
}