package irden.space.proxy.plugin.site.web.rest.v1.dto.player_uuid;


import java.util.regex.Pattern;

public record PlayerUuidParam(String uuid) {
    public static final String NAME = "uuid";
    public static final String PATH = "/{" + NAME + "}";

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-z]{32}$");

    public PlayerUuidParam {
        if (uuid == null || !UUID_PATTERN.matcher(uuid).matches()) {
            throw new IllegalArgumentException("Некорректный UUID игрока");
        }
    }
}