package irden.space.proxy.plugin.irden.integration.web.client;

import irden.space.proxy.plugin.irden.IrdenConfig;
import irden.space.proxy.plugin.irden.integration.web.client.exceptions.IrdenAppClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({IrdenConfig.class})
public class IrdenAppConfiguration {

    @Bean
    RestClient irdenRestClient(IrdenConfig properties
    ) {
        RestClient.Builder builder = RestClient.builder();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.siteConnector().connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.siteConnector().readTimeout());

        return builder
                .baseUrl(properties.siteConnector().baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        properties.siteConnector().apiKey()
                )
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (_, response) -> {
                            String responseBody;

                            try {
                                responseBody = new String(
                                        response.getBody().readAllBytes()
                                );
                            } catch (Exception _) {
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


}