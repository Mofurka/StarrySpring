package irden.space.proxy.plugin.runtime_admin;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.PluginRuntimeState;
import irden.space.proxy.plugin.api.PluginRuntimeView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginRuntimeAdminPluginTest {

    @Test
    void formatsPluginsInStableIdOrder() {
        String result = PluginRuntimeAdminPlugin.formatPlugins(List.of(
                view("zeta", PluginRuntimeState.STOPPED),
                view("alpha", PluginRuntimeState.LOADED)
        ));

        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Plugins:",
                        "- alpha [LOADED] v1.0.0",
                        "- zeta [STOPPED] v1.0.0"
                ),
                result
        );
    }

    private PluginRuntimeView view(String id, PluginRuntimeState state) {
        return new PluginRuntimeView(new PluginDescriptor(id, id, "1.0.0", List.of()), state);
    }
}
