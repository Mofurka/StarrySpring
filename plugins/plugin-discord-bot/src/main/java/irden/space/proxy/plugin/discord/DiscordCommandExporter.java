package irden.space.proxy.plugin.discord;

import irden.space.proxy.plugin.command_handler.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class DiscordCommandExporter {

    private DiscordCommandExporter() {
    }

    public static List<CommandData> export(Collection<RegisteredCommand> commands) {
        List<CommandData> result = new ArrayList<>();

        for (RegisteredCommand command : commands) {
            result.add(export(command));
        }

        return result;
    }

    public static SlashCommandData export(RegisteredCommand command) {
        CommandNode root = command.root();

        SlashCommandData slash = Commands.slash(
                normalizeName(root.name()),
                description(command.description(), root.description())
        );

        List<LiteralNode> literalChildren = literalChildren(root);
        List<ArgumentNode<?>> argumentChildren = argumentChildren(root);

        if (!literalChildren.isEmpty() && !argumentChildren.isEmpty()) {
            throw new IllegalArgumentException(
                    "Discord command '/" + root.name() + "' cannot mix root literals and root arguments"
            );
        }

        if (!literalChildren.isEmpty()) {
            exportRootLiterals(slash, root, literalChildren);
            return slash;
        }

        if (!argumentChildren.isEmpty()) {
            slash.addOptions(exportLinearOptions(root));
            return slash;
        }

        return slash;
    }

    private static void exportRootLiterals(
            SlashCommandData slash,
            CommandNode root,
            List<LiteralNode> literalChildren
    ) {
        List<SubcommandData> subcommands = new ArrayList<>();
        List<SubcommandGroupData> groups = new ArrayList<>();

        for (LiteralNode literal : literalChildren) {
            List<LiteralNode> nestedLiterals = literalChildren(literal);
            List<ArgumentNode<?>> argumentChildren = argumentChildren(literal);

            if (!nestedLiterals.isEmpty() && !argumentChildren.isEmpty()) {
                throw new IllegalArgumentException(
                        "Discord subcommand/group node cannot mix literals and arguments: /"
                                + root.name() + " " + literal.name()
                );
            }

            if (!nestedLiterals.isEmpty()) {
                groups.add(exportGroup(root, literal, nestedLiterals));
            } else {
                subcommands.add(exportSubcommand(literal));
            }
        }

        if (!subcommands.isEmpty()) {
            slash.addSubcommands(subcommands);
        }

        if (!groups.isEmpty()) {
            slash.addSubcommandGroups(groups);
        }
    }

    private static SubcommandGroupData exportGroup(
            CommandNode root,
            LiteralNode groupNode,
            List<LiteralNode> subcommandNodes
    ) {
        SubcommandGroupData group = new SubcommandGroupData(
                normalizeName(groupNode.name()),
                description(groupNode.description())
        );

        List<SubcommandData> subcommands = new ArrayList<>();

        for (LiteralNode subcommandNode : subcommandNodes) {
            List<LiteralNode> deeperLiterals = literalChildren(subcommandNode);

            if (!deeperLiterals.isEmpty()) {
                throw new IllegalArgumentException(
                        "Discord supports only: /command group subcommand options. Too deep near: /"
                                + root.name() + " " + groupNode.name() + " " + subcommandNode.name()
                );
            }

            subcommands.add(exportSubcommand(subcommandNode));
        }

        group.addSubcommands(subcommands);
        return group;
    }

    private static SubcommandData exportSubcommand(LiteralNode node) {
        SubcommandData subcommand = new SubcommandData(
                normalizeName(node.name()),
                description(node.description())
        );

        List<OptionData> options = exportLinearOptions(node);

        if (!options.isEmpty()) {
            subcommand.addOptions(options);
        }

        return subcommand;
    }

    private static List<OptionData> exportLinearOptions(CommandNode start) {
        List<OptionData> result = new ArrayList<>();
        CommandNode current = start;

        while (true) {
            List<LiteralNode> literals = literalChildren(current);
            List<ArgumentNode<?>> arguments = argumentChildren(current);

            if (!literals.isEmpty()) {
                throw new IllegalArgumentException(
                        "Discord options cannot have literal children after arguments near node: " + current.name()
                );
            }

            if (arguments.isEmpty()) {
                break;
            }

            if (arguments.size() > 1) {
                throw new IllegalArgumentException(
                        "Discord export supports only linear argument chains near node: " + current.name()
                );
            }

            ArgumentNode<?> argument = arguments.getFirst();

            result.add(exportOption(argument));
            current = argument;
        }

        validateRequiredOrder(result);
        return result;
    }

    private static OptionData exportOption(ArgumentNode<?> argument) {
        OptionData option = new OptionData(
                optionType(argument.type()),
                normalizeName(argument.name()),
                description(argument.description()),
                argument.required()
        );

        boolean hasStaticChoices = applyChoicesIfPossible(option, argument.type());
        if (!hasStaticChoices && option.getType() == OptionType.STRING && argument.type().supportsAutocomplete()) {
            option.setAutoComplete(true);
        }

        return option;
    }

    private static boolean applyChoicesIfPossible(OptionData option, ArgumentType<?> type) {
        if (!(type instanceof EnumArgumentType<?> enumType)) {
            return false;
        }

        for (String choice : enumType.suggestions(null, "")) {
            option.addChoice(choice, choice);
        }

        return true;
    }

    private static OptionType optionType(ArgumentType<?> type) {
        if (type instanceof IntegerArgumentType) {
            return OptionType.INTEGER;
        }

        return OptionType.STRING;
    }

    private static void validateRequiredOrder(List<OptionData> options) {
        boolean seenOptional = false;

        for (OptionData option : options) {
            if (!option.isRequired()) {
                seenOptional = true;
                continue;
            }

            if (seenOptional) {
                throw new IllegalArgumentException(
                        "Discord requires all required options to be before optional options"
                );
            }
        }
    }

    private static List<LiteralNode> literalChildren(CommandNode node) {
        return node.children()
                .stream()
                .filter(LiteralNode.class::isInstance)
                .map(LiteralNode.class::cast)
                .toList();
    }

    private static List<ArgumentNode<?>> argumentChildren(CommandNode node) {
        return new ArrayList<>(
                node.children()
                        .stream()
                        .filter(ArgumentNode.class::isInstance)
                        .map(child -> (ArgumentNode<?>) child)
                        .toList()
        );
    }

    private static String description(String primary) {
        if (primary != null && !primary.isBlank()) {
            return trimDescription(primary);
        }

        return "No description.";
    }

    private static String description(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return trimDescription(primary);
        }

        return description(fallback);
    }

    private static String trimDescription(String value) {
        String trimmed = value.trim();
        return trimmed.length() <= 100 ? trimmed : trimmed.substring(0, 100);
    }

    private static String normalizeName(String name) {
        String normalized = name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "-");

        if (!normalized.matches("^[a-z0-9-]{1,32}$")) {
            throw new IllegalArgumentException(
                    "Invalid Discord command/option name: " + name
                            + ". Use lowercase letters, digits, '-' or '_'."
            );
        }

        return normalized;
    }
}