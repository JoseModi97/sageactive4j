package io.github.josemodi97.sageactive4j.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * A file attached to a Sage Active entity, or produced by a files export.
 * Download and preview paths are short-lived: use them right away.
 */
public final class SageFile extends SageObject {

    public SageFile(Map<String, Object> json) {
        super(json);
    }

    /** Includes the extension, e.g. {@code 98f7adc7dd104cd984f9c113ef9a8e54.pdf}. */
    @Override
    public String getId() { return string("id"); }
    public String getFileName() { return string("fileName"); }
    /** Extension, e.g. {@code pdf}. */
    public String getType() { return string("type"); }
    public String getMimeType() { return string("mimeType"); }
    public Long getSize() { return longValue("size"); }
    public LocalDate getBusinessDate() { return date("businessDate"); }
    public String getComment() { return string("comment"); }
    public OffsetDateTime getUploadDate() { return dateTime("uploadDate"); }
    public String getEntityType() { return string("entityType"); }
    public String getEntityId() { return string("entityId"); }
    /** Relative, temporary download path. */
    public String getDownloadPath() { return string("downloadPath"); }
    /** Relative, temporary in-browser preview path. */
    public String getPreviewPath() { return string("previewPath"); }
}
