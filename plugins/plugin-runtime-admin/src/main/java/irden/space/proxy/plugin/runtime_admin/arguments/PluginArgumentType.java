package irden.space.proxy.plugin.runtime_admin.arguments;

import irden.space.proxy.plugin.command_handler.ArgumentParseException;
import irden.space.proxy.plugin.command_handler.ArgumentType;
import irden.space.proxy.plugin.command_handler.CommandContext;
import irden.space.proxy.plugin.runtime_admin.PluginRuntimeAdminPlugin;

import java.util.List;
import java.util.function.Supplier;

public class PluginArgumentType implements ArgumentType<String> {
    private final Supplier<PluginRuntimeAdminPlugin> pluginRuntimeAdminPluginSupplier;

    private PluginArgumentType(Supplier<PluginRuntimeAdminPlugin> pluginRuntimeAdminPluginSupplier) {
        this.pluginRuntimeAdminPluginSupplier = pluginRuntimeAdminPluginSupplier;
    }


    public static PluginArgumentType pluginName(PluginRuntimeAdminPlugin pluginRuntimeAdminPlugin) {
        return new PluginArgumentType(() -> pluginRuntimeAdminPlugin);
    }

    @Override
    public String parse(String input) {
        if (input == null || input.isBlank()) {
            throw new ArgumentParseException("Plugin name must not be blank");
        }
        String normalizedInput = input.trim();
        PluginRuntimeAdminPlugin pluginRuntimeAdminPlugin = pluginRuntimeAdminPluginSupplier.get();
        if (pluginRuntimeAdminPlugin == null) {
            throw new ArgumentParseException("Plugin runtime admin plugin is not initialized yet");
        }
        return pluginRuntimeAdminPlugin.findPluginId(normalizedInput);
    }

    @Override
    public String displayName() {
        return "plugin name";
    }
    @Override
    public boolean supportsAutocomplete() {
        return true;
    }


    @Override
    public List<String> suggestions(CommandContext context, String input) throws ArgumentParseException {
        PluginRuntimeAdminPlugin pluginRuntimeAdminPlugin = pluginRuntimeAdminPluginSupplier.get();
        if (pluginRuntimeAdminPlugin == null) {
            return List.of();
        }
        String normalizedInput = input.trim();
        return pluginRuntimeAdminPlugin.searchPlugins(normalizedInput,25);
    }

}
