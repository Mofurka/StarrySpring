package irden.space.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Locale;

@SpringBootApplication(scanBasePackages = {
        "irden.space.boot",
        "irden.space.proxy.application",
        "irden.space.proxy.adapters"
})
public class ServerBootApplication {
    static void main(String[] args) {
        Locale.setDefault(Locale.of(System.getenv("LOCALE") != null ? System.getenv("LOCALE") : "ru"));
        ConfigurableApplicationContext ctx = SpringApplication.run(ServerBootApplication.class, args);
        ctx.registerShutdownHook();
    }
}
