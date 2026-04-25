package irden.space.proxy.plugin.command_handler;

import java.util.List;
import java.util.Objects;

public record RegisteredCommand(
        String ownerPluginId,
        String name,
        List<String> aliases,
        String description,
        CommandSpec spec
) {

    public RegisteredCommand {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(aliases, "aliases");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(spec, "spec");

        aliases = List.copyOf(aliases);
    }

    public CommandNode root() {
        return spec.root();
    }
}