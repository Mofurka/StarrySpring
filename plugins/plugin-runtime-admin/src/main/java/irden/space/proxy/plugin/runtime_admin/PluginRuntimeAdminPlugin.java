package irden.space.proxy.plugin.runtime_admin;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.RegisterPluginPermissions;
import irden.space.proxy.plugin.command_handler.ChatCommand;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.command_handler.CommandSpec;
import irden.space.proxy.plugin.command_handler.StringArgumentType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static irden.space.proxy.plugin.command_handler.CommandSpec.argument;
import static irden.space.proxy.plugin.command_handler.CommandSpec.literal;

@PluginDefinition(
        id = "plugin-runtime-admin",
        name = "Plugin Runtime Admin",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        description = "Administrative commands for plugin runtime management."
)
@Component
public final class PluginRuntimeAdminPlugin implements ProxyPlugin {

    private PluginRuntimeService runtimeService;

    @RegisterPluginPermissions
    public Class<? extends PluginRuntimeAdminPermissions> registerPermissions() {
        return PluginRuntimeAdminPermissions.class;
    }

    @OnLoad
    public void handleLoad(PluginContext context) {
        runtimeService = context.requireService(PluginRuntimeService.class);
    }

    @ChatCommand(value = "plugin", description = "Manage runtime plugins")
    public CommandSpec pluginCommand() {
        return literal("plugin")
                .then(literal("list")
                        .permission(PluginRuntimeAdminPermissions.LIST.permission())
                        .executes(context -> context.reply(formatPlugins(runtimeService.plugins()))))
                .then(operation("start", PluginRuntimeAdminPermissions.START, pluginId -> runtimeService.startPlugin(pluginId)))
                .then(operation("stop", PluginRuntimeAdminPermissions.STOP, pluginId -> runtimeService.stopPlugin(pluginId)))
                .then(operation("reload", PluginRuntimeAdminPermissions.RELOAD, pluginId -> runtimeService.reloadPlugin(pluginId)))
                .build();
    }

    static String formatPlugins(List<PluginRuntimeView> plugins) {
        if (plugins.isEmpty()) {
            return "No plugins available.";
        }

        StringBuilder result = new StringBuilder("Plugins:");
        plugins.stream()
                .sorted(Comparator.comparing(view -> view.descriptor().id()))
                .forEach(view -> result.append(System.lineSeparator())
                        .append("- ")
                        .append(view.descriptor().id())
                        .append(" [")
                        .append(view.state())
                        .append("] v")
                        .append(view.descriptor().version()));
        return result.toString();
    }

    private irden.space.proxy.plugin.command_handler.LiteralBuilder operation(
            String operation,
            PluginRuntimeAdminPermissions permission,
            PluginOperation pluginOperation
    ) {
        return literal(operation)
                .permission(permission.permission())
                .then(argument("pluginId", StringArgumentType.word())
                        .executes(context -> executeOperation(context, operation, pluginOperation)));
    }

    private void executeOperation(CommandContext context, String operation, PluginOperation pluginOperation) {
        String pluginId = context.get("pluginId", String.class);
        List<String> affectedPlugins = pluginOperation.execute(pluginId);
        String affected = affectedPlugins.isEmpty() ? "none" : String.join(", ", affectedPlugins);
        context.reply("Plugin " + operation + " completed. Affected: " + affected);
    }

    @FunctionalInterface
    private interface PluginOperation {
        List<String> execute(String pluginId);
    }
}
