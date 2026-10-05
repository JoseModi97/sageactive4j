package io.github.josemodi97.sageactive4j.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonWriterTest {

    enum Direction { ASC, DESC }

    @Test
    void writesGraphQLVariables() {
        Map<String, Object> line = new LinkedHashMap<String, Object>();
        line.put("quantity", new BigDecimal("2"));
        line.put("unitPrice", new BigDecimal("150.00"));
        Map<String, Object> vars = new LinkedHashMap<String, Object>();
        vars.put("customerId", "c-1");
        vars.put("documentDate", LocalDate.of(2026, 10, 5));
        vars.put("lines", Arrays.asList(line));
        vars.put("order", Direction.DESC);
        vars.put("draft", true);
        vars.put("note", null);

        assertEquals("{\"customerId\":\"c-1\",\"documentDate\":\"2026-10-05\","
                + "\"lines\":[{\"quantity\":2,\"unitPrice\":150.00}],\"order\":\"DESC\",\"draft\":true,\"note\":null}",
                JsonWriter.write(vars));
    }

    @Test
    void bigDecimalNeverUsesScientificNotation() {
        assertEquals("1000", JsonWriter.write(new BigDecimal("1E+3")));
        assertEquals("0.00000001", JsonWriter.write(new BigDecimal("1E-8")));
        assertEquals("1500.0", JsonWriter.write(1.5e3));
        assertEquals("0.00000010", JsonWriter.write(1e-7));
    }

    @Test
    void escapesStrings() {
        assertEquals("\"q\\\"b\\\\n\\n\\t\\u0001\\u2028é\"", JsonWriter.write("q\"b\\n\n\t\u0001 é"));
    }

    @Test
    void javaTimeIsIso8601WithSecondsAlwaysPresent() {
        assertEquals("\"2026-10-05T08:30:00Z\"",
                JsonWriter.write(OffsetDateTime.of(2026, 10, 5, 8, 30, 0, 0, ZoneOffset.UTC)));
        assertEquals("\"2026-10-05T08:30:00+02:00\"", JsonWriter.write(
                ZonedDateTime.of(2026, 10, 5, 8, 30, 0, 0, ZoneId.of("Europe/Paris"))));
        assertEquals("\"2026-10-05T08:30:00Z\"",
                JsonWriter.write(Instant.parse("2026-10-05T08:30:00Z")));
        assertEquals("\"2026-10-05T08:30:00\"", JsonWriter.write(LocalDateTime.of(2026, 10, 5, 8, 30)));
        assertEquals("\"08:30:00\"", JsonWriter.write(LocalTime.of(8, 30)));
        assertEquals("\"2026-10-05\"", JsonWriter.write(LocalDate.of(2026, 10, 5)));
    }

    @Test
    void jsonSerializableIsInlined() {
        JsonSerializable custom = () -> Collections.singletonMap("id", "x");
        assertEquals("[{\"id\":\"x\"}]", JsonWriter.write(Arrays.asList(custom)));
    }

    @Test
    void rejectsUnrepresentableValues() {
        assertThrows(IllegalArgumentException.class, () -> JsonWriter.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> JsonWriter.write(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> JsonWriter.write(new Object()));
    }

    @Test
    void roundTripsThroughTheReader() {
        String json = "{\"a\":[1,2.5,\"x\",{\"b\":null,\"c\":false}],\"d\":\"\\u00e9\\n\"}";
        assertEquals(JsonReader.parse(json), JsonReader.parse(JsonWriter.write(JsonReader.parse(json))));
    }

    @Test
    void prettyPrints() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("a", Arrays.asList(1, 2));
        m.put("b", Collections.emptyMap());
        assertEquals("{\n  \"a\": [\n    1,\n    2\n  ],\n  \"b\": {}\n}", JsonWriter.writePretty(m));
    }
}
