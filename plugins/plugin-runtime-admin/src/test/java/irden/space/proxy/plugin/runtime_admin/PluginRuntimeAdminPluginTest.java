package irden.space.proxy.plugin.runtime_admin;

import irden.space.proxy.plugin.api.PluginDescriptor;
import irden.space.proxy.plugin.api.PluginFailure;
import irden.space.proxy.plugin.api.PluginRuntimeState;
import irden.space.proxy.plugin.api.PluginRuntimeView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void formatsPluginInfoWithStateAndFailure() {
        Instant timestamp = Instant.parse("2026-01-02T03:04:05Z");
        PluginRuntimeView view = new PluginRuntimeView(
                new PluginDescriptor("ban-manager", "Ban Manager", "1.0.0", List.of("player-manager")),
                PluginRuntimeState.FAILED,
                new PluginFailure("ban-manager", "RELOAD", "connection refused", timestamp)
        );

        String result = PluginRuntimeAdminPlugin.formatPluginInfo("ban-manager", List.of(view));

        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Plugin 'ban-manager':",
                        "- name: Ban Manager",
                        "- version: 1.0.0",
                        "- state: FAILED",
                        "- dependsOn: [player-manager]",
                        "- last failure: [RELOAD] connection refused (at 2026-01-02T03:04:05Z)"
                ),
                result
        );
    }

    @Test
    void formatsPluginInfoForUnknownPlugin() {
        String result = PluginRuntimeAdminPlugin.formatPluginInfo("missing", List.of());
        assertTrue(result.contains("not found"));
    }

    private PluginRuntimeView view(String id, PluginRuntimeState state) {
        return new PluginRuntimeView(new PluginDescriptor(id, id, "1.0.0", List.of()), state);
    }
}
