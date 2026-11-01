package irden.space.boot.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import irden.space.proxy.plugin.api.PluginContext;
import irden.space.proxy.plugin.runtime.PluginCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PluginLoggingInitializer {

    private static final Logger log = LoggerFactory.getLogger(PluginLoggingInitializer.class);

    private static final String PLUGIN_CONFIG_SOURCE_PREFIX = "plugin-config[";

    private static final String CONFIG_PREFIX = "logging";
    private static final String APPENDER_NAME_PREFIX = "plugin-";

    private static final String DEFAULT_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{40} - %msg%n";
    private static final FileSize DEFAULT_MAX_FILE_SIZE = new FileSize(DataSize.ofMegabytes(10).toBytes());
    private static final FileSize DEFAULT_TOTAL_SIZE_CAP = new FileSize(DataSize.ofGigabytes(1).toBytes());
    private static final int DEFAULT_MAX_HISTORY = 7;

    private final Map<String, String> fileOwners = new ConcurrentHashMap<>();


    public record PluginLogSettings(
            String file,
            String logger,
            String level,
            Boolean additive,
            String pattern,
            DataSize maxFileSize,
            Integer maxHistory,
            DataSize totalSizeCap,
            Boolean cleanHistoryOnStart
    ) {

        boolean hasWork() {
            return isSet(file) || isSet(level);
        }

        private static boolean isSet(String value) {
            return value != null && !value.isBlank();
        }
    }

    public void apply(
            AnnotationConfigApplicationContext pluginContext,
            PluginCandidate candidate,
            PluginContext scopedContext
    ) {
        String pluginId = candidate.descriptor().id();

        PluginLogSettings settings = readSettings(pluginContext, pluginId);
        if (settings == null || !settings.hasWork()) {
            return;
        }

        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            log.warn(
                    "Logging backend is not Logback; the 'logging' section of plugin '{}' is ignored",
                    pluginId
            );
            return;
        }

        String loggerName = PluginLogSettings.isSet(settings.logger())
                ? settings.logger().trim()
                : candidate.pluginClass().getPackageName();

        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
        List<Runnable> rollback = new ArrayList<>();

        applyLevel(logger, settings, pluginId, rollback);
        applyFileAppender(loggerContext, logger, settings, pluginId, rollback);

        if (rollback.isEmpty()) {
            return;
        }

        scopedContext.onRemove(() -> {
            for (int i = rollback.size() - 1; i >= 0; i--) {
                rollback.get(i).run();
            }
        });
    }

    private PluginLogSettings readSettings(AnnotationConfigApplicationContext pluginContext, String pluginId) {
        String sourcePrefix = PLUGIN_CONFIG_SOURCE_PREFIX + pluginId + "]";

        List<PropertySource<?>> ownSources = new ArrayList<>();
        for (PropertySource<?> source : pluginContext.getEnvironment().getPropertySources()) {
            if (source.getName() != null && source.getName().startsWith(sourcePrefix)) {
                ownSources.add(source);
            }
        }

        if (ownSources.isEmpty()) {
            return null;
        }

        try {
            return new Binder(ConfigurationPropertySources.from(ownSources))
                    .bind(CONFIG_PREFIX, Bindable.of(PluginLogSettings.class))
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Failed to read the 'logging' section of plugin '{}'", pluginId, e);
            return null;
        }
    }

    private void applyLevel(
            ch.qos.logback.classic.Logger logger,
            PluginLogSettings settings,
            String pluginId,
            List<Runnable> rollback
    ) {
        if (!PluginLogSettings.isSet(settings.level())) {
            return;
        }

        Level level = Level.toLevel(settings.level().trim(), null);
        if (level == null) {
            log.warn("Plugin '{}' declares unknown log level '{}'; ignored", pluginId, settings.level());
            return;
        }

        Level previousLevel = logger.getLevel();
        logger.setLevel(level);
        rollback.add(() -> logger.setLevel(previousLevel));

        log.info("Log level of '{}' set to {} by plugin '{}'", logger.getName(), level, pluginId);
    }

    private void applyFileAppender(
            LoggerContext loggerContext,
            ch.qos.logback.classic.Logger logger,
            PluginLogSettings settings,
            String pluginId,
            List<Runnable> rollback
    ) {
        if (!PluginLogSettings.isSet(settings.file())) {
            return;
        }

        String file = settings.file().trim();
        String owner = fileOwners.putIfAbsent(file, pluginId);
        if (owner != null && !owner.equals(pluginId)) {
            log.warn(
                    "Plugin '{}' logs to {}, but plugin '{}' already writes there;"
                            + " two appenders on one file damage it",
                    pluginId, file, owner
            );
        }

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setCharset(StandardCharsets.UTF_8);
        encoder.setPattern(PluginLogSettings.isSet(settings.pattern()) ? settings.pattern() : DEFAULT_PATTERN);

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(loggerContext);
        appender.setName(APPENDER_NAME_PREFIX + pluginId);
        appender.setFile(file);
        appender.setAppend(true);
        appender.setEncoder(encoder);

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
        policy.setContext(loggerContext);
        policy.setParent(appender);
        policy.setFileNamePattern(rotatedFileNamePattern(file));
        policy.setMaxFileSize(settings.maxFileSize() == null
                ? DEFAULT_MAX_FILE_SIZE
                : new FileSize(settings.maxFileSize().toBytes()));
        policy.setMaxHistory(settings.maxHistory() == null ? DEFAULT_MAX_HISTORY : settings.maxHistory());
        policy.setTotalSizeCap(settings.totalSizeCap() == null
                ? DEFAULT_TOTAL_SIZE_CAP
                : new FileSize(settings.totalSizeCap().toBytes()));
        policy.setCleanHistoryOnStart(Boolean.TRUE.equals(settings.cleanHistoryOnStart()));
        appender.setRollingPolicy(policy);

        encoder.start();
        policy.start();
        appender.start();

        boolean previousAdditive = logger.isAdditive();
        logger.addAppender(appender);
        if (settings.additive() != null) {
            logger.setAdditive(settings.additive());
        }

        rollback.add(() -> {
            logger.detachAppender(appender);
            logger.setAdditive(previousAdditive);
            appender.stop();
            policy.stop();
            encoder.stop();
            fileOwners.remove(file, pluginId);
        });

        log.info("Plugin '{}' writes logger '{}' to {}", pluginId, logger.getName(), file);
    }

    private static String rotatedFileNamePattern(String file) {
        String base = file.endsWith(".log")
                ? file.substring(0, file.length() - ".log".length())
                : file;
        return base + ".%d{yyyy-MM-dd}.%i.log.gz";
    }
}
