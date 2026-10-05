package io.github.josemodi97.sageactive4j.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * The answer to a files export request. The ZIP archives are generated
 * asynchronously; list them afterwards with {@code files().listExports(...)}.
 */
public final class FileExportRequest extends SageObject {

    public FileExportRequest(Map<String, Object> json) {
        super(json);
    }

    /** {@code ACCEPTED} or {@code NO_MATCH}. */
    public String getStatus() { return string("status"); }
    public String getMessage() { return string("message"); }
    public OffsetDateTime getRequestedAt() { return dateTime("requestedAt"); }

    public boolean isAccepted() { return "ACCEPTED".equals(getStatus()); }
}
