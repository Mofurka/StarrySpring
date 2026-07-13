package irden.space.proxy.plugin.template;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "template",
        name = "Template Plugin",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = ""
)
@Component
public final class TemplatePlugin implements ProxyPlugin {

    private static final Logger log = LoggerFactory.getLogger(TemplatePlugin.class);

    private final TemplateConfig config;

    public TemplatePlugin(TemplateConfig config) {
        this.config = config;
    }

    @OnLoad
    public void onPluginLoad() {
        log.info("[template] config loaded: greeting='{}', featureEnabled={}, maxItems={}",
                config.getGreeting(), config.isFeatureEnabled(), config.getMaxItems());
    }
}
