package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import lombok.Builder;

import java.util.Arrays;
import java.util.Objects;

@Builder
public record EntityMessage(
        EntityMessageTarget entityId,
        String message,
        VariantValue[] args,
        StarUuid uuid,
        int fromConnection
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityMessage other)) {
            return false;
        }
        return fromConnection == other.fromConnection
                && Objects.equals(entityId, other.entityId)
                && Objects.equals(message, other.message)
                && Arrays.equals(args, other.args)
                && Objects.equals(uuid, other.uuid);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(entityId, message, uuid, fromConnection);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }

    @Override
    public String toString() {
        return "EntityMessage[entityId=" + entityId
                + ", message=" + message
                + ", args=" + Arrays.toString(args)
                + ", uuid=" + uuid
                + ", fromConnection=" + fromConnection
                + ']';
    }
}
