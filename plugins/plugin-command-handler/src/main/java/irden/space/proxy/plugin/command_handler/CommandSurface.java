package irden.space.proxy.plugin.command_handler;

import java.util.Locale;


public record CommandSurface(String id) {

    public static final CommandSurface IN_GAME = new CommandSurface("in-game");

    public static final CommandSurface DISCORD = new CommandSurface("discord");

    static final CommandSurface NONE = new CommandSurface("none");

    public CommandSurface {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command surface id must not be blank");
        }
        id = id.trim().toLowerCase(Locale.ROOT);
    }
}
