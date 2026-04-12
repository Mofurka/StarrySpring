package irden.space.proxy.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginDefinition {

    String id();

    String name();

    String version();

    String description() default "";

    String author() default "";

    String[] dependsOn() default {};
}
