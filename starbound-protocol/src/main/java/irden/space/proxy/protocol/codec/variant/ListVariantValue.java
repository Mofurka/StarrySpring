package irden.space.proxy.protocol.codec.variant;

import java.util.List;

public record ListVariantValue(VariantValue[] values) implements VariantValue {
}
