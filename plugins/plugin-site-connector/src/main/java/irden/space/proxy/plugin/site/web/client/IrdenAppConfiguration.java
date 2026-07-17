package irden.space.proxy.plugin.site.web.client;

import irden.space.proxy.plugin.site.persistence.config.SiteConnectorConfig;
import irden.space.proxy.plugin.site.web.client.exceptions.IrdenAppClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;


import java.net.http.HttpClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({SiteConnectorConfig.class})
public class IrdenAppConfiguration {

    @Bean
    RestClient userRestClient(SiteConnectorConfig properties
    ) {
        RestClient.Builder builder = RestClient.builder();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        properties.apiKey()
                )
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (_, response) -> {
                            String responseBody;

                            try {
                                responseBody = new String(
                                        response.getBody().readAllBytes()
                                );
                            } catch (Exception e) {
                                responseBody =
                                        "<failed to read response body>";
                            }

                            throw new IrdenAppClientException(
                                    response.getStatusCode(),
                                    "Service returned HTTP "
                                            + response.getStatusCode(),
                                    responseBody
                            );
                        }
                )
                .build();
    }

    @Bean
    IrdenAppClient irdenAppLinkerClient(RestClient userRestClient) {
        RestClientAdapter adapter =
                RestClientAdapter.create(userRestClient);

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory
                        .builderFor(adapter)
                        .build();

        return factory.createClient(IrdenAppClient.class);
    }
}