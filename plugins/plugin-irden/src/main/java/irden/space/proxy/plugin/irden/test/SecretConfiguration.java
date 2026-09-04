package irden.space.proxy.plugin.irden.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class SecretConfiguration {

    @Bean
    public SecretKey discordApplicationTokenKey(
            @Value("${irden.discord.application-token-key:k22mnRUld3dU26X3R5M8yfgJV4Fdk53Q1mWOxXPN+24=}") String encodedKey
    ) {
        byte[] key = Base64.getDecoder().decode(encodedKey);

        if (key.length != 32) {
            throw new IllegalArgumentException(
                    "Discord application token key must be 256 bit"
            );
        }

        return new SecretKeySpec(key, "AES");
    }
}
