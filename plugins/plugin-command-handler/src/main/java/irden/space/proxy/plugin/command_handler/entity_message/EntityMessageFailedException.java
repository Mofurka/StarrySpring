package irden.space.proxy.plugin.command_handler.entity_message;

import lombok.Getter;

@Getter
public class EntityMessageFailedException extends RuntimeException {

    /** Имя сообщения, на которое пришла ошибка. */
    private final String messageName;

    /** Текст ошибки от сущности. */
    private final String error;

    public EntityMessageFailedException(String messageName, String error) {
        super("EntityMessage '" + messageName + "' failed: " + error);
        this.messageName = messageName;
        this.error = error;
    }
}
