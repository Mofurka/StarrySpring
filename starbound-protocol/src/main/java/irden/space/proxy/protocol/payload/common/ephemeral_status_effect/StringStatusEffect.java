package irden.space.proxy.protocol.payload.common.ephemeral_status_effect;

public record StringStatusEffect(
        String name
) implements EphemeralStatusEffect {
}
