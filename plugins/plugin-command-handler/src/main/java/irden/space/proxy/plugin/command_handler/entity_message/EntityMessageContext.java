package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessageTarget;

import java.util.Optional;

/**
 * Контекст входящего EntityMessage, передаётся в метод, помеченный {@link EntityMessageHandler}.
 *
 * @param session        сессия игрока, приславшего сообщение
 * @param message        имя сообщения
 * @param args           аргументы (в игре это всегда список/tuple)
 * @param uuid           uuid сообщения - на него уйдёт ответ
 * @param target         кому адресовано (entityId либо unique entity uuid)
 * @param fromConnection id соединения отправителя
 */
public record EntityMessageContext(
        PluginSessionContext session,
        String message,
        VariantValue[] args,
        StarUuid uuid,
        EntityMessageTarget target,
        int fromConnection
) {

    public int argCount() {
        return args == null ? 0 : args.length;
    }

    public VariantValue arg(int index) {
        if (args == null || index < 0 || index >= args.length) {
            return null;
        }
        return args[index];
    }

    public Optional<VariantValue> optionalArg(int index) {
        return Optional.ofNullable(arg(index));
    }
}
