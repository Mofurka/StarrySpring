package irden.space.proxy.application.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "assets.store")
public class GameAssetStoreProperties {

    private boolean enabled = false;
    private List<String> archives = new ArrayList<>();
    private List<String> excludedExtensions = new ArrayList<>(List.of(".ogg", ".png"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getArchives() {
        return archives;
    }

    public void setArchives(List<String> archives) {
        this.archives = archives;
    }

    public List<String> getExcludedExtensions() {
        return excludedExtensions;
    }

    public void setExcludedExtensions(List<String> excludedExtensions) {
        this.excludedExtensions = excludedExtensions;
    }
}
