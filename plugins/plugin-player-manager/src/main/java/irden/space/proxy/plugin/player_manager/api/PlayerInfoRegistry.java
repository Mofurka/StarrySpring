package irden.space.proxy.plugin.player_manager.api;

import irden.space.proxy.plugin.player_manager.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Component
public class PlayerInfoRegistry {

    private final List<PlayerInfoContributor> contributors = new CopyOnWriteArrayList<>();

    public void register(PlayerInfoContributor contributor) {
        Objects.requireNonNull(contributor, "contributor");
        contributors.add(contributor);
        log.info("Registered player info contributor '{}'", safeTitle(contributor));
    }

    public void unregister(PlayerInfoContributor contributor) {
        if (contributor == null) {
            return;
        }
        if (contributors.remove(contributor)) {
            log.info("Unregistered player info contributor '{}'", safeTitle(contributor));
        }
    }


    public List<PlayerInfoSection> collect(Player player) {
        if (player == null) {
            return List.of();
        }

        List<PlayerInfoContributor> ordered = new ArrayList<>(contributors);
        ordered.sort(Comparator.comparingInt(PlayerInfoContributor::order));

        List<PlayerInfoSection> sections = new ArrayList<>(ordered.size());
        for (PlayerInfoContributor contributor : ordered) {
            PlayerInfoSection section = collectOne(contributor, player);
            if (section != null && !section.isEmpty()) {
                sections.add(section);
            }
        }
        return sections;
    }

    private PlayerInfoSection collectOne(PlayerInfoContributor contributor, Player player) {
        try {
            List<PlayerInfoField> fields = contributor.contribute(player);
            return new PlayerInfoSection(safeTitle(contributor), fields);
        } catch (RuntimeException e) {
            log.warn("Player info contributor '{}' failed for uuid={}", safeTitle(contributor), player.uuid(), e);
            return new PlayerInfoSection(
                    safeTitle(contributor),
                    List.of(PlayerInfoField.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()))
            );
        }
    }

    private String safeTitle(PlayerInfoContributor contributor) {
        try {
            String title = contributor.title();
            return title == null || title.isBlank() ? contributor.getClass().getSimpleName() : title;
        } catch (RuntimeException e) {
            return contributor.getClass().getSimpleName();
        }
    }
}
