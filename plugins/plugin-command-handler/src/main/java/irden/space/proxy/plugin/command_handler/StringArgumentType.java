package irden.space.proxy.plugin.command_handler;

public final class StringArgumentType implements ArgumentType<String> {

    private final boolean greedy;

    private StringArgumentType(boolean greedy) {
        this.greedy = greedy;
    }

    public static StringArgumentType word() {
        return new StringArgumentType(false);
    }

    public static StringArgumentType greedyString() {
        return new StringArgumentType(true);
    }

    @Override
    public String parse(String input) {
        if (input == null || input.isBlank()) {
            throw new ArgumentParseException("String argument must not be blank");
        }
        return input;
    }

    @Override
    public boolean greedy() {
        return greedy;
    }

    @Override
    public String displayName() {
        return greedy ? "text" : "word";
    }
}
