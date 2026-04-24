package irden.space.proxy.plugin.command_handler;

import java.util.List;

public interface ArgumentType<T> {

    T parse(String input) throws ArgumentParseException;

    default String displayName() {
        return getClass().getSimpleName();
    }

    default boolean greedy() {
        return false;
    }

    default List<String> suggestions(CommandContext context, String prefix) {
        return List.of();
    }
}