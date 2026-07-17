package irden.space.boot.pluginfixture;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fixture")
public class ScannedConfigProperties {

    private String value = "";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
