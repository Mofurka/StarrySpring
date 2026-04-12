package irden.space.proxy.plugin.command_handler;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@AllArgsConstructor
public class CommandRegistry {
    private final Map<String, Callable<?>> commandMap = new HashMap<>();

}
