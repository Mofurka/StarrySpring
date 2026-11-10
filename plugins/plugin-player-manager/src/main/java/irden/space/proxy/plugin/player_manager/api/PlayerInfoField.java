package irden.space.proxy.plugin.player_manager.api;


public record PlayerInfoField(String name, String value) {

    public static PlayerInfoField of(String name, Object value) {
        return new PlayerInfoField(name, value == null ? "-" : String.valueOf(value));
    }
}
