package irden.space.proxy.plugin.irden.integration.web.client.linker;

import irden.space.proxy.plugin.irden.integration.web.client.IrdenAppConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Import({IrdenAppConfiguration.class})
public class DefaultIrdenAppLinkerClient {

    // Надо придумать как заставить IDE видить что это бин в этом контексте
    @Bean
    IrdenAppLinkerClient irdenAppLinkerClient(RestClient irdenRestClient) {
        RestClientAdapter adapter =
                RestClientAdapter.create(irdenRestClient);

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory
                        .builderFor(adapter)
                        .build();

        return factory.createClient(IrdenAppLinkerClient.class);
    }
}
