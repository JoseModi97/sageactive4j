package io.github.josemodi97.sageactive4j.input;

import java.time.LocalDate;

/**
 * A new sales quote ({@code SalesQuoteCreateGLDtoInput}). Requires a
 * customer and at least one line.
 */
public final class SalesQuoteInput extends SageInput<SalesQuoteInput> {

    public SalesQuoteInput customerId(String customerId) { return set("customerId", customerId); }
    public SalesQuoteInput documentDate(LocalDate documentDate) { return set("documentDate", documentDate); }
    public SalesQuoteInput comments(String comments) { return set("comments", comments); }
    public SalesQuoteInput externalReference(String externalReference) { return set("externalReference", externalReference); }

    /** Lines keep the order they are added in. */
    public SalesQuoteInput addLine(SalesQuoteLineInput line) {
        line.validate();
        return add("lines", line);
    }

    @Override
    public void validate() {
        requireFields("customerId", "lines");
    }
}
