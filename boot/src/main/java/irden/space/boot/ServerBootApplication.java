package irden.space.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "irden.space")
public class ServerBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerBootApplication.class, args);
    }
}
