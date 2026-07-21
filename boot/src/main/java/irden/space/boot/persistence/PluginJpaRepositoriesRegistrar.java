package irden.space.boot.persistence;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.util.Streamable;

import java.lang.annotation.Annotation;


final class PluginJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    private final String basePackage;

    PluginJpaRepositoriesRegistrar(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    protected @NonNull Class<? extends Annotation> getAnnotation() {
        return EnableJpaRepositories.class;
    }

    @Override
    protected @NonNull Class<?> getConfiguration() {
        return EnableJpaRepositoriesConfiguration.class;
    }

    @Override
    protected @NonNull RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new JpaRepositoryConfigExtension();
    }

    @Override
    protected @NonNull Streamable<String> getBasePackages() {
        return Streamable.of(basePackage);
    }

    @EnableJpaRepositories()
    private static final class EnableJpaRepositoriesConfiguration {
    }
}
