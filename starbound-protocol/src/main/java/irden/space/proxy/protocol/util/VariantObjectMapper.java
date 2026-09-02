package irden.space.proxy.protocol.util;

import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;

import java.util.Objects;


@RequiredArgsConstructor
public final class VariantObjectMapper {
    private final ObjectMapper objectMapper;

    private static MapVariantValue requireMap(VariantValue value) {
        if (value instanceof MapVariantValue mapValue) {
            return mapValue;
        }

        throw new IllegalArgumentException(
                "The serialized root value is not an object: "
                        + value.getClass().getSimpleName()
        );
    }

    public VariantValue toVariant(Object value) {
        return toVariant(value, objectMapper.writer());
    }

    public VariantValue toVariant(Object value, Class<?> serializationView) {
        Objects.requireNonNull(serializationView, "serializationView");
        return toVariant(value, objectMapper.writerWithView(serializationView));
    }

    public VariantValue toVariant(Object value, ObjectWriter writer) {
        Objects.requireNonNull(writer, "writer");


        byte[] json = writer.writeValueAsBytes(value);
        JsonNode node = objectMapper.readTree(json);
        return MapVariantUtils.jsonNodeToVariant(node);
    }

    public MapVariantValue toMapVariant(Object value) {
        return requireMap(toVariant(value));
    }

    public MapVariantValue toMapVariant(Object value, ObjectWriter writer) {
        return requireMap(toVariant(value, writer));
    }

    public <T> T fromVariant(VariantValue value, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        return fromVariant(value, objectMapper.readerFor(targetType));
    }

    public <T> T fromVariant(VariantValue value, TypeReference<T> targetType) {
        Objects.requireNonNull(targetType, "targetType");
        return fromVariant(value, objectMapper.readerFor(targetType));
    }

    public <T> T fromVariant(VariantValue value, JavaType targetType) {
        Objects.requireNonNull(targetType, "targetType");
        return fromVariant(value, objectMapper.readerFor(targetType));
    }

    public <T> T fromVariant(
            VariantValue value,
            Class<T> targetType,
            Class<?> deserializationView
    ) {
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(deserializationView, "deserializationView");

        ObjectReader reader = objectMapper.readerWithView(deserializationView)
                .forType(targetType);
        return fromVariant(value, reader);
    }

    public <T> T fromVariant(VariantValue value, ObjectReader reader) {
        Objects.requireNonNull(reader, "reader");

        if (reader.getValueType() == null) {
            throw new IllegalArgumentException("ObjectReader must have a target type");
        }

        JsonNode node = MapVariantUtils.variantToJsonNode(value);
        return reader.readValue(node);
    }
}
