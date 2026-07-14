package irden.space.proxy.plugin.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "template")
public class TemplateConfig {

    private String greeting = "Hello from the template plugin";
    private boolean featureEnabled = true;
    private int maxItems = 10;

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public void setFeatureEnabled(boolean featureEnabled) {
        this.featureEnabled = featureEnabled;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }
}
