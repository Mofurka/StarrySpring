package irden.space.proxy.protocol.payload.common.ephemeral_status_effect;

public record JsonStatusEffect(
        String name,
        float duration
) implements EphemeralStatusEffect {
}
