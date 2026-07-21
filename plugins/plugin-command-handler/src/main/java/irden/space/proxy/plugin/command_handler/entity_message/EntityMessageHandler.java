package irden.space.proxy.plugin.command_handler.entity_message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>{@code
 * @EntityMessageHandler("getBalance")
 * public VariantValue balance(EntityMessageContext ctx) {
 *     return new IntVariantValue(100);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EntityMessageHandler {

    /** Имя сообщения (как в игре), сравнивается точно, с учётом регистра. */
    String value();

    String description() default "";
}
