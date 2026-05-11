package irden.space.proxy.plugin.command_handler;

import java.util.List;

public interface ArgumentType<T> {

    T parse(String input) throws ArgumentParseException;

    default T parse(CommandArgumentContext context, String input) throws ArgumentParseException {
        return parse(input);
    }

    default String displayName() {
        return getClass().getSimpleName();
    }

    default boolean greedy() {
        return false;
    }

    // Discord support and maybe SCC too in the future
    default boolean supportsAutocomplete() {
        return false;
    }

    default List<String> suggestions(CommandContext context, String prefix) {
        return List.of();
    }
}