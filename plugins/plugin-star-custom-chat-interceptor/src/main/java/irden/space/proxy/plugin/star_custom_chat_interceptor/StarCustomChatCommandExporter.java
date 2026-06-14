package irden.space.proxy.plugin.star_custom_chat_interceptor;

import irden.space.proxy.plugin.api.PermissionView;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;

import java.util.*;

public final class StarCustomChatCommandExporter {

	private static final String DEFAULT_DESCRIPTION = "No description.";

	private StarCustomChatCommandExporter() {
	}

	public static ListVariantValue export(Collection<RegisteredCommand> commands) {
		return export(commands, PermissionView.EMPTY);
	}

	public static ListVariantValue export(Collection<RegisteredCommand> commands, PermissionView permissions) {
		Objects.requireNonNull(commands, "commands");

		PermissionView effectivePermissions = permissions == null ? PermissionView.EMPTY : permissions;
		List<VariantValue> exportedCommands = new ArrayList<>();

		for (RegisteredCommand command : commands) {
			MapVariantValue exportedCommand = export(command, effectivePermissions);
			if (exportedCommand != null) {
				exportedCommands.add(exportedCommand);
			}
		}

		return new ListVariantValue(exportedCommands.toArray(VariantValue[]::new));
	}


	public static MapVariantValue export(RegisteredCommand command, PermissionView permissions) {
		Objects.requireNonNull(command, "command");
		PermissionView effectivePermissions = permissions == null ? PermissionView.EMPTY : permissions;

		CommandNode root = command.root();
		if (!hasAccess(root, effectivePermissions)) {
			return null;
		}

		return exportNode(
				root,
				description(command.description(), root.description()),
				"/" + root.name(),
				effectivePermissions
		);
	}

	private static List<MapVariantValue> exportChildren(CommandNode parent, PermissionView permissions) {
		List<MapVariantValue> exportedChildren = new ArrayList<>();

		for (CommandNode child : accessibleChildren(parent, permissions)) {
			exportedChildren.addAll(exportChildNodes(child, permissions));
		}

		return List.copyOf(exportedChildren);
	}

	private static List<MapVariantValue> exportChildNodes(CommandNode child, PermissionView permissions) {
		if (child instanceof ArgumentNode<?> argument && argument.type() instanceof EnumArgumentType<?>) {
			return exportEnumArgument(argument, permissions);
		}

		MapVariantValue exportedChild = exportNode(child, description(child.description()), renderSegment(child), permissions);
		return exportedChild == null ? List.of() : List.of(exportedChild);
	}

	private static List<MapVariantValue> exportEnumArgument(ArgumentNode<?> argument, PermissionView permissions) {
		List<MapVariantValue> exportedChoices = new ArrayList<>();

		for (String choice : argument.type().suggestions(null, "")) {
			MapVariantValue exportedChoice = exportNode(
					argument,
					description(argument.description()),
					choice,
					permissions
			);
			if (exportedChoice != null) {
				exportedChoices.add(exportedChoice);
			}
		}

		return List.copyOf(exportedChoices);
	}

	private static MapVariantValue exportNode(
			CommandNode node,
			String nodeDescription,
			String renderedCommand,
			PermissionView permissions
	) {
		if (!hasAccess(node, permissions)) {
			return null;
		}

		List<MapVariantValue> subcommands = exportChildren(node, permissions);
		if (!node.hasExecutor() && subcommands.isEmpty()) {
			return null;
		}

		return commandEntry(renderedCommand, nodeDescription, subcommands);
	}

	private static List<CommandNode> accessibleChildren(CommandNode parent, PermissionView permissions) {
		List<CommandNode> accessibleChildren = new ArrayList<>();

		for (CommandNode child : parent.children()) {
			if (hasAccess(child, permissions)) {
				accessibleChildren.add(child);
			}
		}

		return List.copyOf(accessibleChildren);
	}

	private static boolean hasAccess(CommandNode node, PermissionView permissions) {
		return !node.hasRequiredPermissions() || permissions.hasAll(node.requiredPermissions());
	}

	private static String renderSegment(CommandNode node) {
		if (node instanceof LiteralNode literal) {
			return literal.name();
		}

		if (node instanceof ArgumentNode<?> argument) {
			return argument.required()
					? "<" + argument.name() + ">"
					: "[" + argument.name() + "]";
		}

		return node.name();
	}

	private static MapVariantValue commandEntry(String command, String description, List<MapVariantValue> subcommands) {
		Map<String, VariantValue> entry = new LinkedHashMap<>();
		entry.put("command", new StringVariantValue(command));
		entry.put("description", new StringVariantValue(description(description)));

		if (!subcommands.isEmpty()) {
			entry.put("subcommands", new ListVariantValue(subcommands.toArray(VariantValue[]::new)));
		}

		return new MapVariantValue(entry);
	}

	private static String description(String primary) {
		return hasText(primary) ? primary.trim() : DEFAULT_DESCRIPTION;
	}

	private static String description(String primary, String fallback) {
		return hasText(primary) ? primary.trim() : description(fallback);
	}


	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}


}
