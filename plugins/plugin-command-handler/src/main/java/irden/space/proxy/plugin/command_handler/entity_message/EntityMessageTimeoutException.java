package irden.space.proxy.plugin.command_handler.entity_message;

import lombok.Getter;

import java.time.Duration;


@Getter
public class EntityMessageTimeoutException extends RuntimeException {

    private final String messageName;
    private final Duration timeout;

    public EntityMessageTimeoutException(String messageName, Duration timeout) {
        super("EntityMessage '" + messageName + "' timed out after " + timeout);
        this.messageName = messageName;
        this.timeout = timeout;
    }
}
