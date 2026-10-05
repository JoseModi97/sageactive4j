package io.github.josemodi97.sageactive4j.input;

import java.time.LocalDate;

/**
 * A new sales order ({@code SalesOrderCreateGLDtoInput}). Requires a
 * customer and at least one line.
 */
public final class SalesOrderInput extends SageInput<SalesOrderInput> {

    public SalesOrderInput customerId(String customerId) { return set("customerId", customerId); }
    public SalesOrderInput documentDate(LocalDate documentDate) { return set("documentDate", documentDate); }
    public SalesOrderInput comments(String comments) { return set("comments", comments); }
    public SalesOrderInput externalReference(String externalReference) { return set("externalReference", externalReference); }

    /** Lines keep the order they are added in. */
    public SalesOrderInput addLine(SalesOrderLineInput line) {
        line.validate();
        return add("lines", line);
    }

    @Override
    public void validate() {
        requireFields("customerId", "lines");
    }
}
