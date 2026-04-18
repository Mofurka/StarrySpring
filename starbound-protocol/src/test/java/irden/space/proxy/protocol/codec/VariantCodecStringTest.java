package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.codec.variant.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VariantCodecStringTest {

    @Test
    void testNullToString() {
        VariantValue value = NullVariantValue.INSTANCE;
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("null", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testBooleanToString() {
        VariantValue trueValue = new BooleanVariantValue(true);
        String trueStr = VariantCodec.INSTANCE.toString(trueValue);
        assertEquals("true", trueStr);

        VariantValue parsedTrue = VariantCodec.INSTANCE.fromString(trueStr);
        assertEquals(trueValue, parsedTrue);

        VariantValue falseValue = new BooleanVariantValue(false);
        String falseStr = VariantCodec.INSTANCE.toString(falseValue);
        assertEquals("false", falseStr);

        VariantValue parsedFalse = VariantCodec.INSTANCE.fromString(falseStr);
        assertEquals(falseValue, parsedFalse);
    }

    @Test
    void testIntToString() {
        VariantValue value = new IntVariantValue(42);
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("42", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testDoubleToString() {
        VariantValue value = new DoubleVariantValue(3.14);
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("3.14", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testStringToString() {
        VariantValue value = new StringVariantValue("Hello, World!");
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("\"Hello, World!\"", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testStringWithEscaping() {
        VariantValue value = new StringVariantValue("Hello\nWorld\t\"Test\"");
        String str = VariantCodec.INSTANCE.toString(value);
        assertTrue(str.contains("\\n"));
        assertTrue(str.contains("\\t"));
        assertTrue(str.contains("\\\""));

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testListToString() {
        VariantValue value = new ListVariantValue(List.of(
                new IntVariantValue(1),
                new IntVariantValue(2),
                new IntVariantValue(3)
        ));
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("[1, 2, 3]", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testEmptyListToString() {
        VariantValue value = new ListVariantValue(List.of());
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("[]", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testMapToString() {
        VariantValue value = new MapVariantValue(Map.of(
                "name", new StringVariantValue("John"),
                "age", new IntVariantValue(30)
        ));
        String str = VariantCodec.INSTANCE.toString(value);
        assertTrue(str.contains("\"name\""));
        assertTrue(str.contains("\"John\""));
        assertTrue(str.contains("\"age\""));
        assertTrue(str.contains("30"));

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertInstanceOf(MapVariantValue.class, parsed);
        MapVariantValue mapValue = (MapVariantValue) parsed;
        assertEquals(2, mapValue.value().size());
        assertEquals(new StringVariantValue("John"), mapValue.value().get("name"));
        assertEquals(new IntVariantValue(30), mapValue.value().get("age"));
    }

    @Test
    void testEmptyMapToString() {
        VariantValue value = new MapVariantValue(Map.of());
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("{}", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testNestedStructure() {
        VariantValue value = new MapVariantValue(Map.of(
                "user", new MapVariantValue(Map.of(
                        "name", new StringVariantValue("Alice"),
                        "active", new BooleanVariantValue(true)
                )),
                "scores", new ListVariantValue(List.of(
                        new IntVariantValue(100),
                        new IntVariantValue(200),
                        new IntVariantValue(300)
                ))
        ));

        String str = VariantCodec.INSTANCE.toString(value);
        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);

        assertInstanceOf(MapVariantValue.class, parsed);
        MapVariantValue map = (MapVariantValue) parsed;

        assertInstanceOf(MapVariantValue.class, map.value().get("user"));
        assertInstanceOf(ListVariantValue.class, map.value().get("scores"));

        MapVariantValue user = (MapVariantValue) map.value().get("user");
        assertEquals(new StringVariantValue("Alice"), user.value().get("name"));
        assertEquals(new BooleanVariantValue(true), user.value().get("active"));

        ListVariantValue scores = (ListVariantValue) map.value().get("scores");
        assertEquals(3, scores.values().size());
    }

    @Test
    void testNegativeNumber() {
        VariantValue value = new IntVariantValue(-42);
        String str = VariantCodec.INSTANCE.toString(value);
        assertEquals("-42", str);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertEquals(value, parsed);
    }

    @Test
    void testScientificNotation() {
        VariantValue value = new DoubleVariantValue(1.23e10);
        String str = VariantCodec.INSTANCE.toString(value);

        VariantValue parsed = VariantCodec.INSTANCE.fromString(str);
        assertInstanceOf(DoubleVariantValue.class, parsed);
        assertEquals(1.23e10, ((DoubleVariantValue) parsed).value(), 0.0001);
    }
}

