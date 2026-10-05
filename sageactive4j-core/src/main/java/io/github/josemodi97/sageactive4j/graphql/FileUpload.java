package io.github.josemodi97.sageactive4j.graphql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A file to send with a GraphQL multipart request. Immutable.
 *
 * <p>The content is held in memory (so a rate-limited upload can be retried
 * as-is), capped at {@value #MAX_SIZE_BYTES} bytes.
 */
public final class FileUpload {

    /** 100 MiB. */
    public static final int MAX_SIZE_BYTES = 100 * 1024 * 1024;

    private final String fileName;
    private final String contentType;
    private final byte[] content;

    private FileUpload(String fileName, String contentType, byte[] content) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File is " + content.length + " bytes; the limit is " + MAX_SIZE_BYTES);
        }
        this.fileName = fileName.trim();
        this.contentType = contentType == null || contentType.trim().isEmpty()
                ? guessContentType(this.fileName) : contentType.trim();
        this.content = content;
    }

    /** Content type guessed from the extension. */
    public static FileUpload of(String fileName, byte[] content) {
        return new FileUpload(fileName, null, content.clone());
    }

    public static FileUpload of(String fileName, String contentType, byte[] content) {
        return new FileUpload(fileName, contentType, content.clone());
    }

    /** Reads the whole file now. */
    public static FileUpload of(Path path) {
        try {
            long size = Files.size(path);
            if (size > MAX_SIZE_BYTES) {
                throw new IllegalArgumentException(path + " is " + size + " bytes; the limit is " + MAX_SIZE_BYTES);
            }
            return new FileUpload(path.getFileName().toString(), null, Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    /** Reads (but does not close) the stream now. */
    public static FileUpload of(String fileName, String contentType, InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_SIZE_BYTES) {
                    throw new IllegalArgumentException("Stream exceeds the " + MAX_SIZE_BYTES + "-byte limit");
                }
                out.write(buf, 0, n);
            }
            return new FileUpload(fileName, contentType, out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the upload stream", e);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    /** A copy of the content. */
    public byte[] getContent() {
        return content.clone();
    }

    public int getSize() {
        return content.length;
    }

    static String guessContentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String ext = dot < 0 ? "" : lower.substring(dot + 1);
        switch (ext) {
            case "pdf": return "application/pdf";
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "tif":
            case "tiff": return "image/tiff";
            case "xml": return "application/xml";
            case "csv": return "text/csv";
            case "txt": return "text/plain";
            case "zip": return "application/zip";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default: return "application/octet-stream";
        }
    }

    @Override
    public String toString() {
        return "FileUpload{" + fileName + ", " + contentType + ", " + content.length + " bytes}";
    }
}
