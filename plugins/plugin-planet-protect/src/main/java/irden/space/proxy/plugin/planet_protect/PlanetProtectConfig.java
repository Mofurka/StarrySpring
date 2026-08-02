package irden.space.proxy.plugin.planet_protect;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planet-protect")
public record PlanetProtectConfig(

) {
}
