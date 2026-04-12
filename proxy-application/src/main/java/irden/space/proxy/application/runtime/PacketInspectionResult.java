package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;

public record PacketInspectionResult(
        Object parsed,
        SessionTransportMode negotiatedTransportMode,
        Integer negotiatedOpenProtocolVersion
) {
    public static PacketInspectionResult empty() {
        return new PacketInspectionResult(null, null, null);
    }

    public boolean negotiatedZstd() {
        return negotiatedTransportMode == SessionTransportMode.ZSTD;
    }
}
