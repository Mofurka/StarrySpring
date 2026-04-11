package irden.space.proxy.application.runtime;


public record PacketInspectionResult(
        Object parsed,
        boolean negotiatedZstd,
        boolean shouldLogPayload
) {
    public static PacketInspectionResult empty() {
        return new PacketInspectionResult(null, false, false);
    }
}
