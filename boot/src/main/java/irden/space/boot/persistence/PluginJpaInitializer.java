package irden.space.boot.persistence;

import irden.space.proxy.plugin.runtime.PluginCandidate;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Поднимает JPA-инфраструктуру ВНУТРИ контекста плагина, если у плагина есть {@code @Entity}:
 * {@link LocalContainerEntityManagerFactoryBean} (сканирует пакет плагина, делит общую
 * {@link DataSource}, схема = {@code <plugin-id с _ вместо ->}), {@link JpaTransactionManager}
 * и Spring Data репозитории ({@link PluginJpaRepositoriesRegistrar}).
 *
 * <p>Так плагин пишет только {@code @Entity} + интерфейс {@code extends JpaRepository}, без JPA-конфига.
 * DDL остаётся за Liquibase ({@code hibernate.hbm2ddl.auto=none}).
 */
public final class PluginJpaInitializer {

    private static final Logger log = LoggerFactory.getLogger(PluginJpaInitializer.class);

    static final String ENTITY_MANAGER_FACTORY_BEAN = "entityManagerFactory";
    static final String TRANSACTION_MANAGER_BEAN = "transactionManager";

    private final ApplicationContext rootContext;

    public PluginJpaInitializer(ApplicationContext rootContext) {
        this.rootContext = Objects.requireNonNull(rootContext, "rootContext");
    }

    public void apply(AnnotationConfigApplicationContext pluginContext, PluginCandidate candidate) {
        String basePackage = candidate.pluginClass().getPackageName();
        if (!hasEntities(pluginContext, basePackage)) {
            return; // у плагина нет JPA-сущностей — ничего не поднимаем
        }

        String pluginId = candidate.descriptor().id();
        String schema = pluginId.replace('-', '_');
        DataSource dataSource = rootContext.getBean(DataSource.class);

        registerEntityManagerFactory(pluginContext, basePackage, schema, dataSource, pluginId);
        registerTransactionManager(pluginContext);
        pluginContext.register(PluginTransactionConfiguration.class);
        registerRepositories(pluginContext, basePackage);

        log.info("Configured JPA for plugin '{}' (schema '{}', entities in '{}')", pluginId, schema, basePackage);
    }

    private boolean hasEntities(AnnotationConfigApplicationContext pluginContext, String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.setResourceLoader(pluginContext);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        return !scanner.findCandidateComponents(basePackage).isEmpty();
    }

    private void registerEntityManagerFactory(
            AnnotationConfigApplicationContext pluginContext,
            String basePackage,
            String schema,
            DataSource dataSource,
            String persistenceUnit
    ) {
        pluginContext.registerBean(
                ENTITY_MANAGER_FACTORY_BEAN,
                LocalContainerEntityManagerFactoryBean.class,
                () -> {
                    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
                    emf.setDataSource(dataSource);
                    emf.setPackagesToScan(basePackage);
                    emf.setPersistenceUnitName(persistenceUnit);
                    emf.setPersistenceProvider(new HibernatePersistenceProvider());

                    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
                    vendorAdapter.setGenerateDdl(false);
                    vendorAdapter.setDatabase(Database.POSTGRESQL);
                    emf.setJpaVendorAdapter(vendorAdapter);

                    Map<String, Object> properties = new HashMap<>();
                    properties.put("hibernate.hbm2ddl.auto", "none"); // DDL — за Liquibase
                    properties.put("hibernate.default_schema", schema);
                    emf.setJpaPropertyMap(properties);
                    return emf;
                },
                PluginJpaInitializer::markAsInfrastructure
        );
    }

    private void registerTransactionManager(AnnotationConfigApplicationContext pluginContext) {
        pluginContext.registerBean(
                TRANSACTION_MANAGER_BEAN,
                JpaTransactionManager.class,
                () -> {
                    JpaTransactionManager transactionManager = new JpaTransactionManager();
                    transactionManager.setEntityManagerFactory(
                            pluginContext.getBean(ENTITY_MANAGER_FACTORY_BEAN, EntityManagerFactory.class));
                    return transactionManager;
                },
                PluginJpaInitializer::markAsInfrastructure
        );
    }


    private static void markAsInfrastructure(BeanDefinition beanDefinition) {
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    }

    private void registerRepositories(AnnotationConfigApplicationContext pluginContext, String basePackage) {
        PluginJpaRepositoriesRegistrar registrar = new PluginJpaRepositoriesRegistrar(basePackage);
        registrar.setBeanFactory(pluginContext.getBeanFactory());
        registrar.setResourceLoader(pluginContext);
        registrar.setEnvironment(pluginContext.getEnvironment());
        registrar.registerBeanDefinitions(
                AnnotationMetadata.introspect(PluginJpaInitializer.class),
                pluginContext,
                AnnotationBeanNameGenerator.INSTANCE
        );
    }
}
