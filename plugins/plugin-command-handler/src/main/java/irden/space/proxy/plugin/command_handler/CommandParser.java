package irden.space.proxy.plugin.command_handler;

import java.util.*;

public final class CommandParser {

    public CommandParseResult parse(
            CommandNode root,
            String argumentsLine,
            List<CommandToken> tokens
    ) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(argumentsLine, "argumentsLine");
        Objects.requireNonNull(tokens, "tokens");

        MatchResult result = match(root, argumentsLine, tokens, 0, new LinkedHashMap<>());

        if (result.success != null) {
            return new CommandParseResult.Success(
                    result.success.executor(),
                    result.success.arguments()
            );
        }

        return new CommandParseResult.Error(result.errorMessage());
    }

    private MatchResult match(
            CommandNode current,
            String argumentsLine,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments
    ) {
        if (index == tokens.size()) {
            if (current.executor() != null) {
                return MatchResult.success(index, current.executor(), arguments);
            }

            MatchResult optionalResult = trySkipOptionalChildren(current, argumentsLine, tokens, index, arguments);
            if (optionalResult.success != null) {
                return optionalResult;
            }

            return MatchResult.failure(
                    index,
                    "Incomplete command. Expected: " + expectedChildren(current)
            );
        }

        CommandToken token = tokens.get(index);

        MatchResult bestFailure = MatchResult.failure(
                index,
                "Unexpected argument '" + token.value() + "'. Expected: " + expectedChildren(current)
        );

        MatchResult literalResult = tryLiteralChildren(current, argumentsLine, tokens, index, arguments);
        if (literalResult.success != null) {
            return literalResult;
        }
        bestFailure = best(bestFailure, literalResult);

        MatchResult argumentResult = tryArgumentChildren(current, argumentsLine, tokens, index, arguments);
        if (argumentResult.success != null) {
            return argumentResult;
        }
        bestFailure = best(bestFailure, argumentResult);

        MatchResult optionalSkipResult = trySkipOptionalChildren(current, argumentsLine, tokens, index, arguments);
        if (optionalSkipResult.success != null) {
            return optionalSkipResult;
        }

        return best(bestFailure, optionalSkipResult);
    }
    private MatchResult tryLiteralChildren(
            CommandNode current,
            String argumentsLine,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments
    ) {
        String token = tokens.get(index).value();

        MatchResult bestFailure = MatchResult.failure(index, "No literal matched");

        for (CommandNode child : current.children()) {
            if (!(child instanceof LiteralNode literal)) {
                continue;
            }

            if (!literal.name().equalsIgnoreCase(token)) {
                continue;
            }

            MatchResult result = match(
                    literal,
                    argumentsLine,
                    tokens,
                    index + 1,
                    copy(arguments)
            );

            if (result.success != null) {
                return result;
            }

            bestFailure = best(bestFailure, result);
        }

        return bestFailure;
    }

    private MatchResult tryArgumentChildren(
            CommandNode current,
            String argumentsLine,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments
    ) {
        MatchResult bestFailure = MatchResult.failure(index, "No argument matched");

        for (CommandNode child : current.children()) {
            if (!(child instanceof ArgumentNode<?> argumentNode)) {
                continue;
            }

            MatchResult result = tryArgument(argumentNode, argumentsLine, tokens, index, arguments);

            if (result.success != null) {
                return result;
            }

            bestFailure = best(bestFailure, result);
        }

        return bestFailure;
    }

    private MatchResult tryArgument(
            ArgumentNode<?> argumentNode,
            String argumentsLine,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments
    ) {
        if (index >= tokens.size()) {
            if (argumentNode.required()) {
                return MatchResult.failure(
                        index,
                        "Missing required argument <" + argumentNode.name() + ">"
                );
            }

            return match(argumentNode, argumentsLine, tokens, index, copy(arguments));
        }

        String rawValue;
        int nextIndex;

        if (argumentNode.type().greedy()) {
            CommandToken firstToken = tokens.get(index);

            rawValue = argumentsLine.substring(firstToken.start()).trim();
            nextIndex = tokens.size();
        } else {
            rawValue = tokens.get(index).value();
            nextIndex = index + 1;
        }

        Object parsedValue;

        try {
            parsedValue = argumentNode.type().parse(rawValue);
        } catch (ArgumentParseException e) {
            return MatchResult.failure(
                    index,
                    "Invalid argument <" + argumentNode.name() + ">: " + e.getMessage()
            );
        } catch (RuntimeException e) {
            return MatchResult.failure(
                    index,
                    "Invalid argument <" + argumentNode.name() + ">: " + rawValue
            );
        }

        LinkedHashMap<String, Object> nextArguments = copy(arguments);
        nextArguments.put(argumentNode.name(), parsedValue);

        return match(argumentNode, argumentsLine, tokens, nextIndex, nextArguments);
    }

    private MatchResult trySkipOptionalChildren(
            CommandNode current,
            String argumentsLine,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments
    ) {
        MatchResult bestFailure = MatchResult.failure(index, "No optional child matched");

        for (CommandNode child : current.children()) {
            if (!(child instanceof ArgumentNode<?> argumentNode)) {
                continue;
            }

            if (argumentNode.required()) {
                continue;
            }

            MatchResult result = match(argumentNode, argumentsLine, tokens, index, copy(arguments));

            if (result.success != null) {
                return result;
            }

            bestFailure = best(bestFailure, result);
        }

        return bestFailure;
    }

    private MatchResult best(MatchResult left, MatchResult right) {
        if (right == null) {
            return left;
        }

        if (left == null) {
            return right;
        }

        if (right.failureIndex > left.failureIndex) {
            return right;
        }

        if (right.failureIndex == left.failureIndex && right.failureMessage.length() > left.failureMessage.length()) {
            return right;
        }

        return left;
    }

    private LinkedHashMap<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private String expectedChildren(CommandNode node) {
        if (node.children().isEmpty()) {
            return "end of command";
        }

        List<String> expected = new ArrayList<>();

        for (CommandNode child : node.children()) {
            if (child instanceof LiteralNode literal) {
                expected.add(literal.name());
            } else if (child instanceof ArgumentNode<?> argument) {
                String value = "<" + argument.name() + ":" + argument.type().displayName() + ">";
                if (!argument.required()) {
                    value = "[" + value + "]";
                }
                expected.add(value);
            }
        }

        return String.join(", ", expected);
    }

    private record Success(
            CommandExecutor executor,
            Map<String, Object> arguments
    ) {
    }

    private record MatchResult(int failureIndex, String failureMessage, Success success) {

        static MatchResult success(
                    int index,
                    CommandExecutor executor,
                    Map<String, Object> arguments
            ) {
                return new MatchResult(
                        index,
                        "",
                        new Success(executor, arguments)
                );
            }

            static MatchResult failure(int index, String message) {
                return new MatchResult(index, message, null);
            }

            String errorMessage() {
                return failureMessage;
            }
        }
}