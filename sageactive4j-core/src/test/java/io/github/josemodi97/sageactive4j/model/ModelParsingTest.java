package io.github.josemodi97.sageactive4j.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ModelParsingTest {

    @Test
    void datesAcceptBothDateAndDateTimeForms() {
        assertEquals(LocalDate.of(2026, 10, 5), SageObject.parseDate("2026-10-05"));
        assertEquals(LocalDate.of(2026, 10, 5), SageObject.parseDate("2026-10-05T00:00:00+02:00"));
        assertNull(SageObject.parseDate("garbage"));
        assertNull(SageObject.parseDate(null));

        assertEquals(OffsetDateTime.of(2025, 9, 18, 9, 41, 12, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18T09:41:12Z"));
        assertEquals(OffsetDateTime.of(2025, 9, 18, 9, 41, 12, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18T09:41:12"));
        assertEquals(OffsetDateTime.of(2025, 9, 18, 0, 0, 0, 0, ZoneOffset.UTC), SageObject.parseDateTime("2025-09-18"));
    }

    @Test
    void missingFieldsAreNullNotErrors() {
        Customer customer = new Customer(Collections.<String, Object>emptyMap());
        assertNull(customer.getCode());
        assertNull(customer.getMainAddress());
        assertTrue(customer.getContacts().isEmpty());
        assertNull(new OpenItem(Collections.<String, Object>emptyMap()).getOutstandingAmount());
    }

    @Test
    void rawAccessForUnmodelledFields() {
        Customer customer = new Customer(JsonReader.parseObject("{\"eInvoicingAddress\":\"123456782_Accounting\",\"x\":{\"y\":1}}"));
        assertEquals("123456782_Accounting", customer.get("eInvoicingAddress"));
        assertEquals(1L, customer.get("x", "y"));
        assertTrue(customer.getRaw().containsKey("x"));
    }

    @Test
    void connectionsParseEdgesAsWellAsNodes() {
        Connection<SageFile> c = Connection.fromJson(JsonReader.parseObject(
                "{\"edges\":[{\"cursor\":\"x\",\"node\":{\"id\":\"f1\"}}],\"pageInfo\":{\"hasNextPage\":true,\"endCursor\":\"x\"}}"),
                SageFile::new);
        assertEquals("f1", c.getNodes().get(0).getId());
        assertNull(c.getTotalCount());
        assertEquals("x", c.getPageInfo().getEndCursor());
    }

    @Test
    void equalityFollowsTheJson() {
        assertEquals(new Product(JsonReader.parseObject("{\"id\":\"p\"}")), new Product(JsonReader.parseObject("{\"id\":\"p\"}")));
    }

    @Test
    void uploadContentTypesAreGuessedFromTheExtension() {
        assertEquals("application/pdf", FileUpload.of("A.PDF", new byte[0]).getContentType());
        assertEquals("image/jpeg", FileUpload.of("scan.jpeg", new byte[0]).getContentType());
        assertEquals("application/octet-stream", FileUpload.of("blob", new byte[0]).getContentType());
        assertEquals("text/x-custom", FileUpload.of("a.pdf", "text/x-custom", new byte[0]).getContentType());
    }
}
