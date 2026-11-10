package irden.space.proxy.plugin.player_manager.api;

import java.util.List;


public record PlayerInfoSection(String title, List<PlayerInfoField> fields) {

    public PlayerInfoSection(String title, List<PlayerInfoField> fields) {
        this.title = title;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }
}
