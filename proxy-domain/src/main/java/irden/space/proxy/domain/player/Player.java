package irden.space.proxy.domain.player;

import java.time.OffsetDateTime;
import java.util.Objects;

public class Player {
    private final PlayerId id;
    private String username;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Player(PlayerId id, String username, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public PlayerId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String username, OffsetDateTime now) {
        this.username = Objects.requireNonNull(username);
        this.updatedAt = Objects.requireNonNull(now);
    }
}