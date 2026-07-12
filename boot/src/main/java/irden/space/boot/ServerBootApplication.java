package irden.space.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "irden.space.boot",
        "irden.space.proxy.application",
        "irden.space.proxy.adapters"
})
public class ServerBootApplication {
    static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ConfigurableApplicationContext ctx = SpringApplication.run(ServerBootApplication.class, args);
        ctx.registerShutdownHook();
    }
}
