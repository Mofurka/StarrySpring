package irden.space.proxy.protocol.codec.variant;

import java.util.Map;

public record MapVariantValue(Map<String, VariantValue> value) implements VariantValue {
}
