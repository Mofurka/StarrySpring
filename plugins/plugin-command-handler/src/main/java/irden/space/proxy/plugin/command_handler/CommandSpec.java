package irden.space.proxy.plugin.command_handler;

import java.util.Objects;

public final class CommandSpec {

    private final CommandNode root;

    public CommandSpec(CommandNode root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public static LiteralBuilder literal(String name) {
        return new LiteralBuilder(name);
    }

    public static <T> ArgumentBuilder<T> argument(String name, ArgumentType<T> type) {
        return new ArgumentBuilder<>(name, type);
    }

    public CommandNode root() {
        return root;
    }
}