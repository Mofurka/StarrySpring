package irden.space.proxy.plugin.command_handler;

public final class IntegerArgumentType implements ArgumentType<Integer> {

    public static IntegerArgumentType integer() {
        return new IntegerArgumentType();
    }

    @Override
    public Integer parse(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new ArgumentParseException("Expected integer, got '" + input + "'", e);
        }
    }

    @Override
    public String displayName() {
        return "integer";
    }
}