package io.github.josemodi97.sageactive4j.input;

import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A file to attach to a Sage Active entity ({@code UploadFileInput}), or to
 * hand to OCR with {@link FileEntityType#AP_AUTOMATION}.
 */
public final class FileAttachment extends SageInput<FileAttachment> {

    private FileUpload file;

    public FileAttachment file(FileUpload file) {
        this.file = file;
        return this;
    }

    public FileAttachment entityType(FileEntityType entityType) { return set("entityType", entityType); }
    /** Required except for {@link FileEntityType#AP_AUTOMATION}. */
    public FileAttachment entityId(String entityId) { return set("entityId", entityId); }
    /** Display name (max 100); defaults to the uploaded file's name. */
    public FileAttachment fileName(String fileName) { return set("fileName", fileName); }
    public FileAttachment businessDate(LocalDate businessDate) { return set("businessDate", businessDate); }
    /** Max 255. */
    public FileAttachment comment(String comment) { return set("comment", comment); }

    public FileUpload getFile() {
        return file;
    }

    @Override
    public void validate() {
        if (file == null) {
            throw new IllegalArgumentException("FileAttachment needs a file(...)");
        }
        requireFields("entityType");
        if (get("entityType") != FileEntityType.AP_AUTOMATION) {
            requireFields("entityId");
        }
    }

    /** The input as sent: {@code file} is {@code null} here, the content travels as a multipart part. */
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("file", null);
        map.putAll(super.toMap());
        return map;
    }

    @Override
    public Object toJsonValue() {
        return toMap();
    }
}
