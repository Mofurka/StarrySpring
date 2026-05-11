package irden.space.proxy.plugin.command_handler;

@FunctionalInterface
public interface CommandContextResolver {

    void resolve(CommandContext.Builder builder);
}

