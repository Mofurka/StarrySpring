package irden.space.proxy.plugin.command_handler;

import java.util.ArrayList;
import java.util.List;

public final class CommandTokenizer {

    private CommandTokenizer() {
    }

    public static List<CommandToken> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<CommandToken> tokens = new ArrayList<>();

        int i = 0;
        int length = input.length();

        while (i < length) {
            while (i < length && Character.isWhitespace(input.charAt(i))) {
                i++;
            }

            if (i >= length) {
                break;
            }

            int start = i;
            StringBuilder value = new StringBuilder();

            if (input.charAt(i) == '"') {
                i++;
                boolean closed = false;

                while (i < length) {
                    char ch = input.charAt(i);

                    if (ch == '\\' && i + 1 < length) {
                        value.append(input.charAt(i + 1));
                        i += 2;
                        continue;
                    }

                    if (ch == '"') {
                        i++;
                        closed = true;
                        break;
                    }

                    value.append(ch);
                    i++;
                }

                if (!closed) {
                    throw new ArgumentParseException("Unclosed quoted argument");
                }

                int end = i;
                tokens.add(new CommandToken(value.toString(), start, end));
                continue;
            }

            while (i < length && !Character.isWhitespace(input.charAt(i))) {
                value.append(input.charAt(i));
                i++;
            }

            tokens.add(new CommandToken(value.toString(), start, i));
        }

        return List.copyOf(tokens);
    }
}