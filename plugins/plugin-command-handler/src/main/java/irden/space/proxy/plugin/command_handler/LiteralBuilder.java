package irden.space.proxy.plugin.command_handler;

import java.util.ArrayList;
import java.util.List;

public final class LiteralBuilder implements CommandNodeBuilder<LiteralNode> {

    private final String name;
    private String description = "";
    private final List<CommandNode> children = new ArrayList<>();
    private CommandExecutor executor;

    LiteralBuilder(String name) {
        this.name = name;
    }

    public LiteralBuilder description(String description) {
        this.description = description;
        return this;
    }

    public LiteralBuilder then(CommandNode node) {
        children.add(node);
        return this;
    }

    public LiteralBuilder then(CommandNodeBuilder<?> builder) {
        children.add(builder.buildNode());
        return this;
    }

    public LiteralBuilder executes(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public CommandSpec build() {
        return new CommandSpec(buildNode());
    }

    @Override
    public LiteralNode buildNode() {
        return new LiteralNode(name, description, children, executor);
    }
}