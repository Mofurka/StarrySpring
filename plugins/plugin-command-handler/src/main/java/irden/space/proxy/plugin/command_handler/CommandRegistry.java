package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.ProxyPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

public class CommandRegistry {

    private static final CommandRegistry GLOBAL = new CommandRegistry();

    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);
    private final Map<String, RegisteredCommand> commandsByName = new LinkedHashMap<>();
    private final Map<String, RegisteredCommand> uniqueCommands = new LinkedHashMap<>();

    public static CommandRegistry global() {
        return GLOBAL;
    }

    public synchronized RegisteredCommand register(
            ProxyPlugin plugin,
            Method method,
            ChatCommand annotation,
            CommandSpec spec
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(annotation, "annotation");
        Objects.requireNonNull(spec, "spec");

        String name = normalizeLabel(annotation.value(), method);
        List<String> aliases = normalizeAliases(annotation.aliases(), method, name);

        if (!spec.root().name().equalsIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "@ChatCommand name '/" + name + "' must match CommandSpec root '" + spec.root().name() + "' on method " + method
            );
        }

        RegisteredCommand command = new RegisteredCommand(
                plugin.descriptor().id(),
                name,
                aliases,
                annotation.description().trim(),
                spec
        );

        registerLabel(name, command);

        for (String alias : aliases) {
            registerLabel(alias, command);
        }

        uniqueCommands.put(name, command);

        log.info("Registered command '/{}' with aliases {} from plugin '{}'",
                name,
                aliases,
                plugin.descriptor().id()
        );

        return command;
    }

    public synchronized RegisteredCommand find(String name) {
        return commandsByName.get(normalizeLookup(name));
    }

    public synchronized Collection<RegisteredCommand> allCommands() {
        return List.copyOf(uniqueCommands.values());
    }

    synchronized void clear() {
        commandsByName.clear();
        uniqueCommands.clear();
    }

    public synchronized String formatHelp() {
        if (uniqueCommands.isEmpty()) {
            return "No commands registered.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("Available commands:");

        uniqueCommands.values().stream()
                .sorted(Comparator.comparing(RegisteredCommand::name))
                .forEach(command -> lines.add(formatLine(command)));

        return String.join("\n", lines);
    }

    private String formatLine(RegisteredCommand command) {
        StringBuilder builder = new StringBuilder("/")
                .append(command.name());

        if (!command.description().isBlank()) {
            builder.append(" - ").append(command.description());
        }

        if (!command.aliases().isEmpty()) {
            builder.append(" (aliases: ")
                    .append(command.aliases().stream().map(alias -> "/" + alias).toList())
                    .append(')');
        }

        return builder.toString();
    }

    private void registerLabel(String label, RegisteredCommand command) {
        log.debug("Registering command label '/{}' for plugin '{}'",
                label,
                command.ownerPluginId()
        );

        RegisteredCommand existing = commandsByName.putIfAbsent(label, command);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Command '/" + label + "' is already registered by plugin '" + existing.ownerPluginId() + "'"
            );
        }
    }

    private List<String> normalizeAliases(String[] aliases, Method method, String name) {
        List<String> normalizedAliases = new ArrayList<>();

        for (String alias : aliases) {
            String normalized = normalizeLabel(alias, method);

            if (normalized.equals(name)) {
                throw new IllegalArgumentException("Alias duplicates command name on method " + method);
            }

            if (normalizedAliases.contains(normalized)) {
                throw new IllegalArgumentException("Duplicate alias '/" + normalized + "' on method " + method);
            }

            normalizedAliases.add(normalized);
        }

        return List.copyOf(normalizedAliases);
    }

    private String normalizeLabel(String rawLabel, Method method) {
        String label = normalizeLookup(rawLabel);

        if (label.isBlank()) {
            throw new IllegalArgumentException("@ChatCommand name must not be blank on method " + method);
        }

        if (label.startsWith("/")) {
            throw new IllegalArgumentException("@ChatCommand name must not start with '/' on method " + method);
        }

        if (label.contains(" ")) {
            throw new IllegalArgumentException("@ChatCommand name must not contain spaces on method " + method);
        }

        return label;
    }

    private String normalizeLookup(String name) {
        Objects.requireNonNull(name, "name");
        return name.trim().toLowerCase(Locale.ROOT);
    }
}