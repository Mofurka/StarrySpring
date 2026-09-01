package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;

import java.util.function.Supplier;


public record PacketInspectionResult(
        Supplier<Object> parsedPayloadSupplier,
        SessionTransportMode negotiatedTransportMode,
        Integer negotiatedOpenProtocolVersion
) {
    private static final Supplier<Object> NO_PAYLOAD = () -> null;

    public static PacketInspectionResult empty() {
        return new PacketInspectionResult(NO_PAYLOAD, null, null);
    }

    public static PacketInspectionResult lazy(Supplier<Object> parsedPayloadSupplier) {
        return new PacketInspectionResult(parsedPayloadSupplier, null, null);
    }


    public Object parsed() {
        return parsedPayloadSupplier.get();
    }

    public boolean negotiatedZstd() {
        return negotiatedTransportMode == SessionTransportMode.ZSTD;
    }
}
