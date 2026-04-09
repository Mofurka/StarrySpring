package irden.space.proxy.protocol.codec.variant;

public sealed interface VariantValue permits
        BooleanVariantValue,
        DoubleVariantValue,
        IntVariantValue,
        ListVariantValue,
        MapVariantValue,
        NullVariantValue,
        StringVariantValue {
}
