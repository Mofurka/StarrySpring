package irden.space.proxy.plugin.irden;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@ConfigurationProperties(prefix = "irden")
public record IrdenConfig() {

}
