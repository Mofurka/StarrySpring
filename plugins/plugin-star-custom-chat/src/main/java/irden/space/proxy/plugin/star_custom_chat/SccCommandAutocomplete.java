package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class SccCommandAutocomplete {

    private static final String DEFAULT_DESCRIPTION = "No description.";
    private static final int MAX_SUGGESTIONS = 25;

    private final CommandHandlerPlugin commandHandler;

    public ListVariantValue suggest(PluginSessionContext session, String rawText) {
        List<VariantValue> entries = new ArrayList<>();
        try {
            collect(session, rawText == null ? "" : rawText, entries);
        } catch (RuntimeException e) {
            log.debug("Failed to build autocomplete for '{}'", rawText, e);
        }
        return Variants.list(entries);
    }

    private void collect(PluginSessionContext session, String text, List<VariantValue> entries) {
        if (!text.startsWith("/")) {
            return;
        }

        List<Token> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return;
        }

        boolean trailingSpace = Character.isWhitespace(text.charAt(text.length() - 1));

        Token commandToken = tokens.getFirst();
        if (!commandToken.value().startsWith("/")) {
            return;
        }
        String commandName = commandToken.value().substring(1).toLowerCase(Locale.ROOT);
        if (commandName.isEmpty()) {
            return;
        }

        if (tokens.size() == 1 && !trailingSpace) {
            return;
        }

        RegisteredCommand command = findCommand(commandName);
        if (command == null) {
            return;
        }

        List<Token> argTokens = tokens.subList(1, tokens.size());
        Token focusedToken = (trailingSpace || argTokens.isEmpty())
                ? null
                : argTokens.getLast();
        List<Token> completeTokens = focusedToken == null
                ? argTokens
                : argTokens.subList(0, argTokens.size() - 1);

        String focused = focusedToken == null ? "" : focusedToken.value();
        String prefix = focusedToken == null ? text : text.substring(0, focusedToken.start());

        PermissionView permissions = session.permissions();

        CommandNode current = command.root();
        List<CommandHandlerPlugin.ResolvedArgument> priorArguments = new ArrayList<>();
        String pendingLabel = null;

        for (Token token : completeTokens) {
            String value = token.value();
            if (pendingLabel != null) {
                ArgumentNode<?> argument = argumentChild(current, pendingLabel);
                if (argument != null) {
                    priorArguments.add(new CommandHandlerPlugin.ResolvedArgument(argument, value));
                    current = argument;
                }
                pendingLabel = null;
                continue;
            }

            String label = token.quoted()
                    ? null
                    : StarCustomChatCommandExporter.argumentLabelName(value);
            if (label != null) {
                pendingLabel = label;
                continue;
            }

            LiteralNode literal = literalChild(current, value);
            if (literal != null) {
                current = literal;
                continue;
            }

            ArgumentNode<?> positional = singleArgumentChild(current);
            if (positional != null) {
                priorArguments.add(new CommandHandlerPlugin.ResolvedArgument(positional, value));
                current = positional;
                continue;
            }

            return;
        }

        if (pendingLabel != null) {
            ArgumentNode<?> argument = argumentChild(current, pendingLabel);
            if (argument != null && hasAccess(argument, permissions)) {
                addDynamicValues(session, commandName, argument, priorArguments, focused, prefix, entries);
            }
            return;
        }

        for (CommandNode child : current.children()) {
            if (entries.size() >= MAX_SUGGESTIONS) {
                break;
            }
            if (!hasAccess(child, permissions)) {
                continue;
            }

            if (child instanceof LiteralNode literal) {
                if (focused.isEmpty() || literal.name().startsWith(focused)) {
                    addEntry(entries, prefix + literal.name(), literal.name(), literal.description());
                }
            } else if (child instanceof ArgumentNode<?> argument) {
                String placeholder = argument.required()
                        ? argument.name() + ":"
                        : argument.name() + "?:";
                if (focused.isEmpty()
                        || placeholder.startsWith(focused)
                        || argument.name().startsWith(focused)) {
                    addEntry(entries, prefix + placeholder, placeholder, argument.description());
                }
            }
        }
    }

    private void addDynamicValues(
            PluginSessionContext session,
            String commandName,
            ArgumentNode<?> argument,
            List<CommandHandlerPlugin.ResolvedArgument> priorArguments,
            String focused,
            String prefix,
            List<VariantValue> entries
    ) {
        if (!argument.type().supportsAutocomplete()) {
            return;
        }

        PacketInterceptionContext packetContext = new PacketInterceptionContext(
                session,
                null,
                new ChatSent("/" + commandName, null, null),
                PacketDirection.TO_SERVER
        );

        List<String> values = commandHandler.suggestArgumentValues(
                packetContext, commandName, priorArguments, argument, focused);

        for (String value : values) {
            if (entries.size() >= MAX_SUGGESTIONS) {
                break;
            }
            if (value != null && !value.isBlank()) {
                addEntry(entries, prefix + value, value, argument.description());
            }
        }
    }

    private RegisteredCommand findCommand(String name) {
        for (RegisteredCommand command : commandHandler.allCommands()) {
            if (command.name().equalsIgnoreCase(name)) {
                return command;
            }
            for (String alias : command.aliases()) {
                if (alias.equalsIgnoreCase(name)) {
                    return command;
                }
            }
        }
        return null;
    }

    private boolean hasAccess(CommandNode node, PermissionView permissions) {
        return node.isExportedTo(CommandSurface.IN_GAME)
                && (!node.hasRequiredPermissions() || permissions.hasAll(node.requiredPermissions()));
    }

    private LiteralNode literalChild(CommandNode parent, String name) {
        for (CommandNode child : parent.children()) {
            if (child instanceof LiteralNode literal && literal.name().equalsIgnoreCase(name)) {
                return literal;
            }
        }
        return null;
    }

    private ArgumentNode<?> argumentChild(CommandNode parent, String name) {
        for (CommandNode child : parent.children()) {
            if (child instanceof ArgumentNode<?> argument && argument.name().equalsIgnoreCase(name)) {
                return argument;
            }
        }
        return null;
    }

    private ArgumentNode<?> singleArgumentChild(CommandNode parent) {
        ArgumentNode<?> found = null;
        for (CommandNode child : parent.children()) {
            if (child instanceof ArgumentNode<?> argument) {
                if (found != null) {
                    return null;
                }
                found = argument;
            }
        }
        return found;
    }

    private void addEntry(List<VariantValue> entries, String fullCommand, String segment, String description) {
        entries.add(Variants.mapBuilder()
                .put("name", fullCommand)
                .put("data", segment)
                .put("description",
                        description == null || description.isBlank()
                                ? DEFAULT_DESCRIPTION
                                : description.trim())
                .build());
    }

    private List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int length = text.length();

        while (i < length) {
            while (i < length && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i >= length) {
                break;
            }

            int start = i;
            StringBuilder value = new StringBuilder();
            boolean quoted = text.charAt(i) == '"';

            if (quoted) {
                i++;
                while (i < length) {
                    char ch = text.charAt(i);
                    if (ch == '\\' && i + 1 < length) {
                        value.append(text.charAt(i + 1));
                        i += 2;
                    } else if (ch == '"') {
                        i++;
                        break; // закрывающая тварь
                    } else {
                        value.append(ch);
                        i++;
                    }
                }
            } else {
                while (i < length && !Character.isWhitespace(text.charAt(i))) {
                    value.append(text.charAt(i));
                    i++;
                }
            }

            tokens.add(new Token(value.toString(), start, quoted));
        }

        return tokens;
    }

    private record Token(String value, int start, boolean quoted) {
    }
}
