package irden.space.boot.config;


import irden.space.proxy.protocol.util.VariantObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }

    @Bean
    public VariantObjectMapper variantObjectMapper(JsonMapper jsonMapper) {
        return new VariantObjectMapper(jsonMapper);
    }

}
