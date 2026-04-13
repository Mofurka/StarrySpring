package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.ProxyPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public record RegisteredCommand(
        String ownerPluginId,
        ProxyPlugin owner,
        Method method,
        String name,
        List<String> aliases,
        String description,
        String usage
) {

    public RegisteredCommand {
        Objects.requireNonNull(ownerPluginId, "ownerPluginId");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(aliases, "aliases");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(usage, "usage");
        aliases = List.copyOf(aliases);
    }

    public void invoke(CommandContext context) {
        try {
            if (method.getParameterCount() == 0) {
                method.invoke(owner);
                return;
            }

            method.invoke(owner, context);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to invoke @ChatCommand method " + method, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke @ChatCommand method " + method, e);
        }
    }
}
