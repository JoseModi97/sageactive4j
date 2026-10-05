package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.GraphQLRequest;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.FileAttachment;
import io.github.josemodi97.sageactive4j.input.FileEntityType;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.FileExportRequest;
import io.github.josemodi97.sageactive4j.model.SageFile;
import java.util.Collections;
import java.util.Map;

/**
 * File attachments: upload, list, bulk export. Uploaded files are virus
 * scanned before they appear in listings (except when looked up by id).
 *
 * <p>The {@code files} query only accepts flat AND filters on
 * {@code entityType}, {@code entityId}, {@code businessDate},
 * {@code uploadDate}, {@code fileName}, {@code type}, {@code id},
 * {@code status} and {@code filesExport}, and sorting on
 * {@code businessDate} only.
 */
public final class FilesClient extends DomainClient {

    public FilesClient(SageActive4jClient client) {
        super(client);
    }

    /**
     * Uploads and attaches a file (GraphQL multipart). Returns the file id,
     * which includes the extension, e.g. {@code 98f7...e54.pdf}.
     */
    public String upload(FileAttachment attachment) {
        validated(attachment, "attachment");
        GraphQLRequest request = GraphQLDocuments.operation("uploadFileToEntity",
                Collections.singletonMap("input", attachment.toMap()));
        Map<String, Object> data = organization("uploadFileToEntity")
                .executeMultipart(request, "variables.input.file", attachment.getFile());
        return JsonReader.getString(require(data, "uploadFileToEntity"), "id");
    }

    /** Files, latest business date first. */
    public Connection<SageFile> list(ListOptions options) {
        return list(organization("files"), "files", options, null, "{ businessDate: DESC }", null, SageFile::new);
    }

    /** Files attached to one record. */
    public Connection<SageFile> listFor(FileEntityType entityType, String entityId, ListOptions options) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType must not be null");
        }
        return list(organization("files"), "files", options,
                "{ entityType: { eq: " + entityType.name() + " }, entityId: { eq: $entityId } }",
                "{ businessDate: DESC }", GraphQLDocuments.var("entityId", "UUID", requireId(entityId, "entityId")),
                SageFile::new);
    }

    public Iterable<SageFile> all(ListOptions options) {
        return Pages.iterate(options, this::list);
    }

    /**
     * Starts generating ZIP archives (max 65 MB each, plus an index) of the
     * files matching {@code whereLiteral}; they appear in
     * {@link #listExports(ListOptions)} once ready.
     *
     * @param language     for the index headers: {@code fr}, {@code en}, {@code es}, {@code de} or e.g. {@code fr-FR}
     * @param whereLiteral e.g. {@code { entityType: { eq: CUSTOMER }, businessDate: { gte: "2026-01-01" } }};
     *                     {@code null} = everything
     */
    public FileExportRequest requestExport(String language, String whereLiteral) {
        String document = GraphQLDocuments.load("filesExport").replace("{{whereArg}}",
                whereLiteral == null || whereLiteral.trim().isEmpty() ? "" : ", where: " + whereLiteral.trim());
        Map<String, Object> data = organization("filesExport").query(new GraphQLRequest(document,
                Collections.singletonMap("language", requireId(language, "language"))));
        return new FileExportRequest(require(data, "filesExport"));
    }

    /** The archives produced by {@link #requestExport(String, String)}; download paths are short-lived. */
    public Connection<SageFile> listExports(ListOptions options) {
        return list(organization("files"), "files", options, "{ filesExport: { eq: true } }", null, null,
                SageFile::new);
    }
}
