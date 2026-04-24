package irden.space.proxy.plugin.command_handler;

import java.util.List;

public interface CommandNode {

    String name();

    String description();

    List<CommandNode> children();

    CommandExecutor executor();

    default boolean hasExecutor() {
        return executor() != null;
    }
}