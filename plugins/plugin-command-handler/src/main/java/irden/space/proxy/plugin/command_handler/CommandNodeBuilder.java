package irden.space.proxy.plugin.command_handler;

public interface CommandNodeBuilder<N extends CommandNode> {

    N buildNode();
}