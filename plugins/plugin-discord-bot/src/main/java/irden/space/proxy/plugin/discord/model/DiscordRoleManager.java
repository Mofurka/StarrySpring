package irden.space.proxy.plugin.discord.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DiscordRoleManager {
    private final Map<String, DiscordRankRoles> ranks;
    private final Path path;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    public DiscordRoleManager() {
        this.path = Path.of("config/discord-ranks.jsonc");
        this.ranks = loadOrCreateConfig();
    }

    private Map<String, DiscordRankRoles> loadOrCreateConfig() {
        return null;
    }


}
