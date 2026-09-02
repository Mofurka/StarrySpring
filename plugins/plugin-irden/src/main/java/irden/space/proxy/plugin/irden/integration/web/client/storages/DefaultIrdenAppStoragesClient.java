package irden.space.proxy.plugin.irden.integration.web.client.storages;

import irden.space.proxy.plugin.irden.integration.web.client.storages.dto.StorageIdParamToStringConverter;
import irden.space.proxy.plugin.irden.integration.web.dto.player_app_id.PlayerAppIdToStringConverter;
import irden.space.proxy.plugin.irden.integration.web.dto.player_discord_id.PlayerDiscordIdToStringConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
//@ComponentScan("irden.space.proxy.plugin.irden.integration.web")
public class DefaultIrdenAppStoragesClient {

    // Надо придумать как заставить IDE видить что это бин в этом контексте
    @Bean
    IrdenAppStoragesClient irdenAppStoragesClient(RestClient irdenRestClient,
                                                  PlayerAppIdToStringConverter playerAppIdToStringConverter,
                                                  StorageIdParamToStringConverter storageIdParamToStringConverter,
                                                  PlayerDiscordIdToStringConverter playerDiscordIdToStringConverter) {
        RestClientAdapter adapter =
                RestClientAdapter.create(irdenRestClient);


        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();

        conversionService.addConverter(playerAppIdToStringConverter);
        conversionService.addConverter(storageIdParamToStringConverter);
        conversionService.addConverter(playerDiscordIdToStringConverter);

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory
                        .builderFor(adapter)
                        .conversionService(conversionService)
                        .build();

        return factory.createClient(IrdenAppStoragesClient.class);
    }
}
