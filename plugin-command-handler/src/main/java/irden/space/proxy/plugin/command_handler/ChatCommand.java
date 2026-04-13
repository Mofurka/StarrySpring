package irden.space.proxy.plugin.command_handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SuppressWarnings("unused")
public @interface ChatCommand {

    String value();

    String[] aliases() default {};

    String description() default "";

    String usage() default "";
}
