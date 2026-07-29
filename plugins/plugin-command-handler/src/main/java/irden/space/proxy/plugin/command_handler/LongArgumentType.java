package irden.space.proxy.plugin.command_handler;

public final class LongArgumentType implements ArgumentType<Long> {

    public static LongArgumentType lng() {
        return new LongArgumentType();
    }

    @Override
    public Long parse(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            throw new ArgumentParseException("Expected long, got '" + input + "'", e);
        }
    }

    @Override
    public String displayName() {
        return "long";
    }
}