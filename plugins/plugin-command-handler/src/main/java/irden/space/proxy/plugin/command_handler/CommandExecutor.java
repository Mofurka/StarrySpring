package irden.space.proxy.plugin.command_handler;

@FunctionalInterface
public interface CommandExecutor {

    void execute(CommandContext context);
}