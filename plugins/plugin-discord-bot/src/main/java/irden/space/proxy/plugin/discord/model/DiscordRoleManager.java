package irden.space.proxy.plugin.discord.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Component
@Lazy
public final class DiscordRoleManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private static final String DEFAULT_CONFIG_RESOURCE = "config/discord-ranks.jsonc";

    private final Path path;
    private final List<DiscordRankRoles> ranks;

    public DiscordRoleManager() {
        this(Path.of("config/plugins/discord-bot/discord-ranks.jsonc"));
    }

    public DiscordRoleManager(Path path) {
        this.path = Objects.requireNonNull(path, "path");
        this.ranks = loadOrCreateConfig();
    }

    public List<String> resolveServerRoleNames(Collection<Long> discordRoleIds) {
        if (discordRoleIds == null || discordRoleIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> distinctRoleIds = new LinkedHashSet<>();
        for (Long discordRoleId : discordRoleIds) {
            if (discordRoleId != null) {
                distinctRoleIds.add(discordRoleId);
            }
        }

        if (distinctRoleIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> resolvedRoleNames = new LinkedHashSet<>();
        for (DiscordRankRoles rank : ranks) {
            if (rank == null || rank.name() == null || rank.name().isBlank()) {
                continue;
            }
            if (distinctRoleIds.contains(rank.roleId())) {
                resolvedRoleNames.add(rank.name().trim());
            }
        }

        return List.copyOf(resolvedRoleNames);
    }

    private List<DiscordRankRoles> loadOrCreateConfig() {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(path)) {
                copyBundledDefault(path);
            }

            if (Files.exists(path)) {
                List<DiscordRankRoles> configuredRanks = OBJECT_MAPPER.readValue(
                        path.toFile(),
                        new TypeReference<>() {
                        }
                );
                return configuredRanks == null ? List.of() : List.copyOf(configuredRanks);
            }

            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), List.of());
            return List.of();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load Discord rank mapping configuration: " + path, ex);
        }
    }

    private void copyBundledDefault(Path target) throws IOException {
        try (InputStream bundled = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (bundled != null) {
                Files.copy(bundled, target);
            }
        }
    }
}
