package irden.space.proxy.plugin.api;

public record Permission(String name, int id) {

    public Permission {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Permission name must not be blank");
        }
        if (id < 0) {
            throw new IllegalArgumentException("Permission id must not be negative");
        }
    }
}
