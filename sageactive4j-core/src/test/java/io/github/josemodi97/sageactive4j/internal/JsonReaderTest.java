package io.github.josemodi97.sageactive4j.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonReaderTest {

    @Test
    void parsesAGraphQLConnectionShape() {
        String json = "{\"data\":{\"salesInvoices\":{\"edges\":[{\"node\":{\"id\":\"a1\",\"totalNet\":120.50,"
                + "\"customer\":{\"code\":\"CUST001\"}}}],\"totalCount\":1,\"pageInfo\":{\"hasNextPage\":false}}}}";
        Map<String, Object> root = JsonReader.parseObject(json);

        assertEquals(1L, JsonReader.getLong(root, "data", "salesInvoices", "totalCount"));
        assertEquals(Boolean.FALSE, JsonReader.getBoolean(root, "data", "salesInvoices", "pageInfo", "hasNextPage"));
        List<Object> edges = JsonReader.getList(root, "data", "salesInvoices", "edges");
        assertEquals(1, edges.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> edge = (Map<String, Object>) edges.get(0);
        assertEquals("CUST001", JsonReader.getString(edge, "node", "customer", "code"));
        assertEquals(new BigDecimal("120.50"), JsonReader.getBigDecimal(edge, "node", "totalNet"));
    }

    @Test
    void keepsDecimalPrecisionExactly() {
        // 0.1 + 0.2 must not become 0.30000000000000004 anywhere in the pipeline.
        Map<String, Object> m = JsonReader.parseObject("{\"a\":0.1,\"b\":0.2,\"c\":1234567890.123456789}");
        assertEquals(new BigDecimal("0.3"), JsonReader.getBigDecimal(m, "a").add(JsonReader.getBigDecimal(m, "b")));
        assertEquals("1234567890.123456789", JsonReader.getString(m, "c"));
    }

    @Test
    void numberTypes() {
        assertEquals(0L, JsonReader.parse("0"));
        assertEquals(-42L, JsonReader.parse("-42"));
        assertEquals(Long.MAX_VALUE, JsonReader.parse("9223372036854775807"));
        assertEquals(new BigInteger("9223372036854775808"), JsonReader.parse("9223372036854775808"));
        assertEquals(new BigDecimal("1.5E+3"), JsonReader.parse("1.5e3"));
        assertEquals(new BigDecimal("-2E-2"), JsonReader.parse("-2E-2"));
    }

    @Test
    void stringEscapesIncludingSurrogatePairs() {
        assertEquals("a\"b\\c/d\b\f\n\r\t", JsonReader.parse("\"a\\\"b\\\\c\\/d\\b\\f\\n\\r\\t\""));
        assertEquals("é", JsonReader.parse("\"\\u00e9\""));
        assertEquals("\uD83D\uDE00", JsonReader.parse("\"\\ud83d\\ude00\""));
        assertEquals("Société Générale €", JsonReader.parse("\"Société Générale €\""));
    }

    @Test
    void literalsAndEmptyContainers() {
        assertEquals(Boolean.TRUE, JsonReader.parse(" true "));
        assertEquals(Boolean.FALSE, JsonReader.parse("false"));
        assertNull(JsonReader.parse("null"));
        assertTrue(JsonReader.parseObject("{ }").isEmpty());
        assertEquals(Arrays.asList(), JsonReader.parse("[ ]"));
        assertEquals(Arrays.asList(1L, "x", null), JsonReader.parse("[1,\"x\",null]"));
    }

    @Test
    void duplicateKeysLastWins() {
        assertEquals("2", JsonReader.getString(JsonReader.parseObject("{\"k\":\"1\",\"k\":\"2\"}"), "k"));
    }

    @Test
    void rejectsMalformedInput() {
        String[] bad = {
            "", "{", "[1,]", "{\"a\":1,}", "{\"a\" 1}", "{a:1}", "01", "1.", "-", "1e", "\"unterminated",
            "\"bad \\x escape\"", "\"\\u12\"", "tru", "nul", "[1] trailing", "{\"a\":1}}", "\"raw\ncontrol\""
        };
        for (String input : bad) {
            assertThrows(JsonReader.JsonParseException.class, () -> JsonReader.parse(input), "should reject: " + input);
        }
    }

    @Test
    void rejectsExcessiveNesting() {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i <= JsonReader.MAX_DEPTH; i++) {
            deep.append('[');
        }
        assertThrows(JsonReader.JsonParseException.class, () -> JsonReader.parse(deep.toString()));
    }

    @Test
    void parseObjectRejectsNonObjects() {
        assertThrows(JsonReader.JsonParseException.class, () -> JsonReader.parseObject("[]"));
        assertThrows(JsonReader.JsonParseException.class, () -> JsonReader.parseObject("null"));
    }

    @Test
    void pathHelpersAreNullSafe() {
        Map<String, Object> m = JsonReader.parseObject("{\"a\":{\"b\":\"x\"},\"n\":\"12\",\"list\":[1]}");
        assertNull(JsonReader.getString(m, "a", "missing"));
        assertNull(JsonReader.getString(m, "a", "b", "deeper"));
        assertNull(JsonReader.getString(m, "a"));
        assertEquals(12L, JsonReader.getLong(m, "n"));
        assertTrue(JsonReader.getList(m, "nope").isEmpty());
        assertNull(JsonReader.getMap(m, "list"));
    }
}
