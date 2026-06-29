package irden.space.boot;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.PluginSpringConfiguration;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.runtime.DefaultPacketInterceptorRegistry;
import irden.space.proxy.plugin.runtime.DefaultPluginContext;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import irden.space.proxy.plugin.runtime.PluginContainer;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginMessageSourceTest {

    @Test
    void injectsPluginScopedMessageSourceIntoPluginBeans() {
        AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
        rootContext.refresh();
        DefaultPluginContext pluginContext = new DefaultPluginContext(new DefaultPacketInterceptorRegistry());

        PluginContainer container = new SpringPluginContainerFactory(rootContext).create(
                PluginCandidate.fromClass(I18nFixturePlugin.class),
                pluginContext.forPlugin("i18n-fixture"),
                List.of()
        );

        Map<String, MessageConsumer> consumers = container.beansOfType(MessageConsumer.class);
        assertEquals(1, consumers.size());
        MessageConsumer consumer = consumers.values().iterator().next();

        assertEquals("Hello", consumer.greeting(Locale.ENGLISH));
        assertEquals("Привет", consumer.greeting(Locale.of("ru")));

        container.close();
        rootContext.close();
    }

    @PluginDefinition(id = "i18n-fixture", name = "I18n Fixture", version = "1.0.0")
    @PluginSpringConfiguration(value = I18nFixtureConfiguration.class, scanPluginPackage = false)
    static final class I18nFixturePlugin implements ProxyPlugin {
    }

    static final class I18nFixtureConfiguration {
        @Bean
        MessageConsumer messageConsumer(MessageSource messageSource) {
            return new MessageConsumer(messageSource);
        }
    }

    static final class MessageConsumer {
        private final MessageSource messageSource;

        MessageConsumer(MessageSource messageSource) {
            this.messageSource = messageSource;
        }

        String greeting(Locale locale) {
            return messageSource.getMessage("greeting", null, locale);
        }
    }
}
