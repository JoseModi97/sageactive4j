package io.github.josemodi97.sageactive4j.input;

/** What an uploaded file is attached to. */
public enum FileEntityType {
    ACCOUNTING_ENTRY,
    /** OCR: Sage Active reads the PDF and creates a purchase invoice from it; no entity id needed. */
    AP_AUTOMATION,
    CUSTOMER,
    EMPLOYEE,
    ORGANIZATION,
    PRODUCT,
    SUPPLIER
}
