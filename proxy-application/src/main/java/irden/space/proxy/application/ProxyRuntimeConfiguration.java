package irden.space.proxy.application;

import irden.space.proxy.application.assets.GameAssetStoreProperties;
import irden.space.proxy.protocol.assets.pak.GameAssetStore;
import irden.space.proxy.protocol.assets.pak.InMemoryGameAssetStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ProxyServerProperties.class, GameAssetStoreProperties.class})
public class ProxyRuntimeConfiguration {

    @Bean
    public GameAssetStore gameAssetStore(GameAssetStoreProperties properties) {
        return new InMemoryGameAssetStore(
                properties.isEnabled(),
                InMemoryGameAssetStore.toPaths(properties.getArchives()),
                properties.getExcludedExtensions()
        );
    }
}
