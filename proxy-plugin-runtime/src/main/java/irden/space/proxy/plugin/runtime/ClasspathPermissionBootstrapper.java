package irden.space.proxy.plugin.runtime;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import irden.space.proxy.plugin.api.PermissionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

public final class ClasspathPermissionBootstrapper implements PermissionBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(ClasspathPermissionBootstrapper.class);
    private static final String[] DEFAULT_PACKAGES = {"irden.space.proxy.plugin"};

    private final String[] basePackages;

    public ClasspathPermissionBootstrapper(String... basePackages) {
        if (basePackages == null || basePackages.length == 0) {
            this.basePackages = DEFAULT_PACKAGES;
            return;
        }

        this.basePackages = Arrays.stream(basePackages)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(packageName -> !packageName.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void bootstrap() {
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(basePackages)
                .enableClassInfo()
                .scan()) {
            for (ClassInfo classInfo : scanResult.getClassesImplementing(PermissionEnum.class.getName())) {
                bootstrapPermissionType(classInfo.loadClass());
            }
        }
    }

    private void bootstrapPermissionType(Class<?> permissionType) {
        if (!permissionType.isEnum()) {
            throw new IllegalStateException(
                    "Permission bootstrap supports only enums implementing PermissionEnum: " + permissionType.getName()
            );
        }

        Object[] enumConstants = permissionType.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            log.debug("Skipping empty permission enum {}", permissionType.getName());
            return;
        }

        ((PermissionEnum) enumConstants[0]).registerDefaults();
        log.debug("Bootstrapped permissions from {}", permissionType.getName());
    }
}
