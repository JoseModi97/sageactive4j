package io.github.josemodi97.sageactive4j.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class DocumentsAndTransportHelpersTest {

    @Test
    void everyBundledDocumentLoadsAndListTemplatesRender() throws IOException {
        String[] lists = {"organizations", "countries", "currencies", "users", "accountingExercises", "accountingAccounts",
            "journalTypes", "accountingEntries", "customers", "suppliers", "products", "salesInvoices",
            "salesInvoiceOpenItems", "purchaseInvoices", "purchaseInvoiceOpenItems", "bankMovements", "paymentMethods", "files",
            "salesQuotes", "salesOrders", "salesTariffs", "taxes", "taxGroups", "taxTreatments", "paymentTerms"};
        for (String name : lists) {
            GraphQLRequest request = GraphQLDocuments.list(name, ListOptions.first(5), null, null, null, null);
            assertFalse(request.getQuery().contains("{{"), name + " left a placeholder");
            assertTrue(request.getQuery().contains("pageInfo { hasNextPage endCursor }"), name);
            assertBalanced(name, request.getQuery());
        }
        String[] operations = {"organizationDetail", "userProfile", "userAccessPolicyCheck", "createAccountingEntryUsingCodes",
            "createCustomer", "productPriceById", "createSalesInvoice", "closeSalesInvoice", "postSalesInvoice",
            "salesOpenItemSettlement", "postPurchaseInvoice", "purchaseOpenItemSettlement", "bankAccounts",
            "reconcileBankMovement", "unReconcileBankMovement", "uploadFileToEntity", "filesExport", "aggregationCatalog",
            "aggregationExecute", "localizedErrorMessage", "createSalesQuote", "createSalesOrder", "generateCreditNote"};
        for (String name : operations) {
            assertBalanced(name, GraphQLDocuments.load(name));
        }
        java.net.URL dir = getClass().getResource("/io/github/josemodi97/sageactive4j/queries/");
        if (dir != null && "file".equals(dir.getProtocol())) { // only countable from the classes dir, not the jar
            try (InputStream listing = dir.openStream()) {
                int files = new Scanner(listing, "UTF-8").useDelimiter("\\A").next().split("\\.graphql").length - 1;
                assertEquals(lists.length + operations.length, files, "a document is not covered by this test");
            }
        }
    }

    @Test
    void unknownDocumentIsAPackagingError() {
        assertThrows(IllegalStateException.class, () -> GraphQLDocuments.load("nope"));
        assertThrows(IllegalStateException.class,
                () -> GraphQLDocuments.list("userProfile", ListOptions.defaults(), null, null, null, null));
    }

    @Test
    void filtersCombineFlat() {
        assertEquals("{ a: 1, b: 2 }", GraphQLDocuments.combine("{ a: 1 }", "{ b: 2 }"));
        assertEquals("{ b: 2 }", GraphQLDocuments.combine("{ }", "{ b: 2 }"));
        assertEquals("{ and: [{ a: 1 }, [weird]] }", GraphQLDocuments.combine("{ a: 1 }", "[weird]"));
    }

    @Test
    void defaultsApplyOnlyWhenTheCallerGivesNone() {
        String q = GraphQLDocuments.list("customers", ListOptions.first(5), null, "{ d: 1 }", "[{ o: ASC }]", null).getQuery();
        assertTrue(q.contains("where: { d: 1 }, order: [{ o: ASC }]"), q);
        q = GraphQLDocuments.list("customers", ListOptions.first(5).where("{ w: 2 }").order("[{ x: DESC }]"),
                null, "{ d: 1 }", "[{ o: ASC }]", null).getQuery();
        assertTrue(q.contains("where: { w: 2 }, order: [{ x: DESC }]"), q);
    }

    @Test
    void mutationDetection() {
        assertTrue(GraphQLTransport.isMutation("mutation { a }"));
        assertTrue(GraphQLTransport.isMutation("  # comment\n mutation($v: Int) { a }"));
        assertTrue(GraphQLTransport.isMutation("﻿,mutation X { a }"));
        assertFalse(GraphQLTransport.isMutation("query { mutationLog }"));
        assertFalse(GraphQLTransport.isMutation("{ a }"));
        assertFalse(GraphQLTransport.isMutation("mutationish { a }"));
        assertFalse(GraphQLTransport.isMutation("# mutation\nquery { a }"));
    }

    @Test
    void sanitizesFileNames() {
        assertEquals("a%22b_c.pdf", GraphQLTransport.sanitizeFileName("a\"b\\c.pdf\r\n"));
    }

    private static void assertBalanced(String name, String document) {
        int depth = 0;
        for (char c : document.toCharArray()) {
            depth += c == '{' ? 1 : c == '}' ? -1 : 0;
            assertTrue(depth >= 0, name + " closes a brace too early");
        }
        // list templates still carry {{...}} placeholders, which balance too
        assertEquals(0, depth, name + " has unbalanced braces");
        assertTrue(new String(document.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).trim().length() > 10);
    }
}
