package irden.space.proxy.plugin.irden;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "irden")
public class IrdenConfig {

}
