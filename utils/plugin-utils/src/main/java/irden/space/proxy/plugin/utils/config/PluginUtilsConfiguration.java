package irden.space.proxy.plugin.utils.config;

import irden.space.proxy.plugin.api.annotations.PluginContextConfiguration;
import irden.space.proxy.plugin.utils.messages.DefaultMessageUtils;
import irden.space.proxy.plugin.utils.messages.MessageUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@PluginContextConfiguration
@Configuration(proxyBeanMethods = false)
public class PluginUtilsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MessageUtils messageUtils(MessageSource messageSource) {
        return new DefaultMessageUtils(messageSource);
    }

}
