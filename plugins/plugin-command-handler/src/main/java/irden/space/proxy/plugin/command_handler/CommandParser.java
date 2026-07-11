package irden.space.proxy.plugin.command_handler;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public final class CommandParser {

    public CommandParseResult parse(
            CommandNode root,
            CommandArgumentContext commandContext,
            List<CommandToken> tokens
    ) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(commandContext, "commandContext");
        Objects.requireNonNull(tokens, "tokens");

        MatchResult result = match(root, commandContext, tokens, 0, new LinkedHashMap<>(), List.of(root));

        if (result.success() != null) {
            return new CommandParseResult.Success(
                    result.success().executor(),
                    result.success().arguments(),
                    result.success().matchedNodes()
            );
        }

        return new CommandParseResult.Error(result.failureMessage());
    }

    private MatchResult match(
            CommandNode current,
            CommandArgumentContext commandContext,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments,
            List<CommandNode> matchedNodes
    ) {
        if (index == tokens.size()) {
            if (current.executor() != null) {
                return MatchResult.success(index, current.executor(), arguments, matchedNodes);
            }

            MatchResult optionalResult = trySkipOptionalChildren(current, commandContext, tokens, index, arguments, matchedNodes);
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

        MatchResult literalResult = tryLiteralChildren(current, commandContext, tokens, index, arguments, matchedNodes);
        if (literalResult.success != null) {
            return literalResult;
        }
        bestFailure = best(bestFailure, literalResult);

        MatchResult argumentResult = tryArgumentChildren(current, commandContext, tokens, index, arguments, matchedNodes);
        if (argumentResult.success != null) {
            return argumentResult;
        }
        bestFailure = best(bestFailure, argumentResult);

        MatchResult optionalSkipResult = trySkipOptionalChildren(current, commandContext, tokens, index, arguments, matchedNodes);
        if (optionalSkipResult.success != null) {
            return optionalSkipResult;
        }

        return best(bestFailure, optionalSkipResult);
    }

    private MatchResult tryLiteralChildren(
            CommandNode current,
            CommandArgumentContext commandContext,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments,
            List<CommandNode> matchedNodes
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
                    commandContext,
                    tokens,
                    index + 1,
                    copy(arguments),
                    appendNode(matchedNodes, literal)
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
            CommandArgumentContext commandContext,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments,
            List<CommandNode> matchedNodes
    ) {
        MatchResult bestFailure = MatchResult.failure(index, "No argument matched");

        for (CommandNode child : current.children()) {
            if (!(child instanceof ArgumentNode<?> argumentNode)) {
                continue;
            }

            MatchResult result = tryArgument(argumentNode, commandContext, tokens, index, arguments, matchedNodes);

            if (result.success() != null) {
                return result;
            }

            bestFailure = best(bestFailure, result);
        }

        return bestFailure;
    }

    private MatchResult tryArgument(
            ArgumentNode<?> argumentNode,
            CommandArgumentContext commandContext,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments,
            List<CommandNode> matchedNodes
    ) {
        if (index >= tokens.size()) {
            if (argumentNode.required()) {
                return MatchResult.failure(
                        index,
                        "Missing required argument <" + argumentNode.name() + ">",
                        true
                );
            }

            return match(argumentNode, commandContext, tokens, index, copy(arguments), appendNode(matchedNodes, argumentNode));
        }

        String rawValue;
        int nextIndex;

        if (argumentNode.type().greedy()) {
            CommandToken firstToken = tokens.get(index);

            rawValue = commandContext.argumentsLine().substring(firstToken.start()).trim();
            nextIndex = tokens.size();
        } else {
            rawValue = tokens.get(index).value();
            nextIndex = index + 1;
        }

        Object parsedValue;

        try {
            parsedValue = argumentNode.type().parse(commandContext.withArguments(arguments), rawValue);
        } catch (ArgumentParseException e) {
            return MatchResult.failure(
                    index,
                    "Invalid argument <" + argumentNode.name() + ">: " + e.getMessage(),
                    true
            );
        } catch (RuntimeException e) {
            return MatchResult.failure(
                    index,
                    "Invalid argument <" + argumentNode.name() + ">: " + rawValue,
                    true
            );
        }

        LinkedHashMap<String, Object> nextArguments = copy(arguments);
        nextArguments.put(argumentNode.name(), parsedValue);

        return match(argumentNode, commandContext, tokens, nextIndex, nextArguments, appendNode(matchedNodes, argumentNode));
    }

    private MatchResult trySkipOptionalChildren(
            CommandNode current,
            CommandArgumentContext commandContext,
            List<CommandToken> tokens,
            int index,
            LinkedHashMap<String, Object> arguments,
            List<CommandNode> matchedNodes
    ) {
        MatchResult bestFailure = MatchResult.failure(index, "No optional child matched");

        for (CommandNode child : current.children()) {
            if (!(child instanceof ArgumentNode<?> argumentNode)) {
                continue;
            }

            if (argumentNode.required()) {
                continue;
            }

            MatchResult result = match(argumentNode, commandContext, tokens, index, copy(arguments), appendNode(matchedNodes, argumentNode));

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

        if (right.failureIndex != left.failureIndex) {
            return right.failureIndex > left.failureIndex ? right : left;
        }

        // На одной и той же позиции конкретная ошибка парсинга аргумента
        // ("Player not found: ...") важнее общего "Unexpected argument ...",
        // иначе осмысленная причина теряется из-за тай-брейка по длине строки.
        if (right.specific != left.specific) {
            return right.specific ? right : left;
        }

        if (right.failureMessage.length() > left.failureMessage.length()) {
            return right;
        }

        return left;
    }

    private LinkedHashMap<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private List<CommandNode> appendNode(List<CommandNode> matchedNodes, CommandNode node) {
        List<CommandNode> nextMatchedNodes = new ArrayList<>(matchedNodes.size() + 1);
        nextMatchedNodes.addAll(matchedNodes);
        nextMatchedNodes.add(node);
        return List.copyOf(nextMatchedNodes);
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
//                String value = "<" + argument.name() + ":" + argument.type().displayName() + ">";
//                if (!argument.required()) {
//                    value = "[" + value + "]";
//                }
//                String value = "<" + argument.name() + ">" + ": " + argument.description();
//                if (!argument.required()) {
//                    value = "[" + value + "]";
//                }
                var sb = new StringBuilder();
                sb.append("<").append(argument.name()).append(": ").append(argument.type().displayName());
                if (!argument.description().isEmpty()) {
                    sb.append(" - ").append(argument.description());
                }
                sb.append(">");
                if (!argument.required()) {
                    sb.insert(0, "[").append("]");
                }
                expected.add(sb.toString());
            }
        }

        return String.join(", ", expected);
    }

    private record Success(
            CommandExecutor executor,
            Map<String, Object> arguments,
            List<CommandNode> matchedNodes
    ) {
    }

    private record MatchResult(int failureIndex, String failureMessage, Success success, boolean specific) {

        static MatchResult success(
                int index,
                CommandExecutor executor,
                Map<String, Object> arguments,
                List<CommandNode> matchedNodes
        ) {
            return new MatchResult(
                    index,
                    "",
                    new Success(executor, arguments, matchedNodes),
                    false
            );
        }

        static MatchResult failure(int index, String message) {
            return failure(index, message, false);
        }

        static MatchResult failure(int index, String message, boolean specific) {
            return new MatchResult(index, message, null, specific);
        }

        String errorMessage() {
            return failureMessage;
        }
    }
}
