package irden.space.proxy.protocol.packet;

import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.SignedVlqCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;
import irden.space.proxy.protocol.payload.registry.PacketParserRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;

public final class PacketEnvelopes {

    private static final PacketParserRegistry PARSER_REGISTRY = new PacketParserRegistry();

    private PacketEnvelopes() {
    }

    public static PacketEnvelope fromPayload(PacketType packetType, Object payload, PacketDirection direction) {
        return fromPayload(packetType, payload, PacketParser.LEGACY_PROTOCOL_VERSION, direction);
    }

    public static PacketEnvelope fromPayload(
            PacketType packetType,
            Object payload,
            int openProtocolVersion,
            PacketDirection direction
    ) {
        return fromPayload(packetType, payload, false, openProtocolVersion, direction);
    }

    public static PacketEnvelope fromPayload(
            PacketType packetType,
            Object payload,
            boolean compressed,
            PacketDirection direction
    ) {
        return fromPayload(packetType, payload, compressed, PacketParser.LEGACY_PROTOCOL_VERSION, direction);
    }

    public static PacketEnvelope fromPayload(
            PacketType packetType,
            Object payload,
            boolean compressed,
            int openProtocolVersion,
            PacketDirection direction
    ) {
        Objects.requireNonNull(packetType, "packetType");
        Objects.requireNonNull(payload, "payload");

        PacketParser<Object> parser = resolveParser(packetType);
        BinaryWriter binaryWriter = new BinaryWriter(openProtocolVersion);
        byte[] payloadBytes = parser.write(binaryWriter, payload);
        return fromRawPayload(packetType.id(), packetType, payloadBytes, compressed, direction);
    }

    public static PacketEnvelope fromRawPayload(PacketType packetType, byte[] payload, PacketDirection direction) {
        return fromRawPayload(packetType, payload, false, direction);
    }

    public static PacketEnvelope fromRawPayload(
            PacketType packetType,
            byte[] payload,
            boolean compressed,
            PacketDirection direction
    ) {
        Objects.requireNonNull(packetType, "packetType");
        return fromRawPayload(packetType.id(), packetType, payload, compressed, direction);
    }

    public static PacketEnvelope fromRawPayload(
            int rawPacketTypeId,
            PacketType packetType,
            byte[] payload,
            boolean compressed,
            PacketDirection direction
    ) {
        Objects.requireNonNull(payload, "payload");

        byte[] parsedPayload = payload.clone();
        byte[] wirePayload = compressed ? compress(parsedPayload) : parsedPayload.clone();
        byte[] originalData = buildOriginalData(rawPacketTypeId, wirePayload, compressed);

        return PacketEnvelope.of(
                rawPacketTypeId,
                packetType,
                wirePayload.length,
                compressed,
                parsedPayload,
                originalData,
                direction
        );
    }

    public static PacketEnvelope rewrite(PacketEnvelope originalEnvelope, Object payload) {
        return rewrite(originalEnvelope, payload, PacketParser.LEGACY_PROTOCOL_VERSION);
    }

    public static PacketEnvelope rewrite(PacketEnvelope originalEnvelope, Object payload, int openProtocolVersion) {
        Objects.requireNonNull(originalEnvelope, "originalEnvelope");
        return fromPayload(
                originalEnvelope.rawPacketTypeId(),
                originalEnvelope.packetType(),
                payload,
                originalEnvelope.compressed(),
                openProtocolVersion,
                originalEnvelope.direction()
        );
    }

    public static PacketEnvelope rewriteRawPayload(PacketEnvelope originalEnvelope, byte[] payload) {
        Objects.requireNonNull(originalEnvelope, "originalEnvelope");
        return fromRawPayload(
                originalEnvelope.rawPacketTypeId(),
                originalEnvelope.packetType(),
                payload,
                originalEnvelope.compressed(),
                originalEnvelope.direction()
        );
    }

    private static PacketEnvelope fromPayload(
            int rawPacketTypeId,
            PacketType packetType,
            Object payload,
            boolean compressed,
            int openProtocolVersion,
            PacketDirection direction
    ) {
        Objects.requireNonNull(payload, "payload");

        PacketParser<Object> parser = resolveParser(packetType);
        BinaryWriter binaryWriter = new BinaryWriter(openProtocolVersion);// Ensure the open protocol version is valid
        byte[] payloadBytes = parser.write(binaryWriter, payload);
        return fromRawPayload(rawPacketTypeId, packetType, payloadBytes, compressed, direction);
    }

    @SuppressWarnings("unchecked")
    private static PacketParser<Object> resolveParser(PacketType packetType) {
        PacketParser<?> parser = PARSER_REGISTRY.get(packetType);
        if (parser == null) {
            throw new IllegalArgumentException("No packet parser registered for packet type " + packetType);
        }
        return (PacketParser<Object>) parser;
    }

    private static byte[] buildOriginalData(int rawPacketTypeId, byte[] wirePayload, boolean compressed) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeByte(rawPacketTypeId);
        SignedVlqCodec.INSTANCE.write(writer, compressed ? -wirePayload.length : wirePayload.length);
        writer.writeBytes(wirePayload);
        return writer.toByteArray();
    }

    private static byte[] compress(byte[] payload) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(payload);
            deflaterOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress packet payload", e);
        }
    }
}
