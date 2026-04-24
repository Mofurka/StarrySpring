package irden.space.proxy.plugin.command_handler;

import java.util.Map;

public sealed interface CommandParseResult permits CommandParseResult.Success, CommandParseResult.Error {

    boolean success();

    record Success(
            CommandExecutor executor,
            Map<String, Object> arguments
    ) implements CommandParseResult {

        public Success {
            arguments = Map.copyOf(arguments);
        }

        @Override
        public boolean success() {
            return true;
        }
    }

    record Error(String message) implements CommandParseResult {

        @Override
        public boolean success() {
            return false;
        }
    }
}