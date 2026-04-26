package irden.space.proxy.plugin.command_handler;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class EnumArgumentType<E extends Enum<E>> implements ArgumentType<E> {

    private final Class<E> enumType;

    private EnumArgumentType(Class<E> enumType) {
        this.enumType = enumType;
    }

    public static <E extends Enum<E>> EnumArgumentType<E> of(Class<E> enumType) {
        return new EnumArgumentType<>(enumType);
    }

    @Override
    public E parse(String input) {
        String normalized = input.trim().toUpperCase(Locale.ROOT);

        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalized)) {
                return constant;
            }
        }

        throw new ArgumentParseException(
                "Expected one of " + suggestions(null, "") + ", got '" + input + "'"
        );
    }

    @Override
    public List<String> suggestions(CommandContext context, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);

        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .filter(name -> name.startsWith(normalizedPrefix))
                .toList();
    }

    @Override
    public String displayName() {
        // instead of name we are using enum constants
        return Arrays.toString(enumType.getEnumConstants()).toLowerCase(Locale.ROOT);

    }
}