package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.FileAttachment;
import io.github.josemodi97.sageactive4j.input.FileEntityType;
import io.github.josemodi97.sageactive4j.model.SageFile;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Lists or uploads files. */
@Command(name = "file", description = "Upload or list file attachments ('file', 'file upload').",
        subcommands = {FileCommand.ListFiles.class, FileCommand.Upload.class})
final class FileCommand extends CliCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return new ListFiles().call();
    }

    /** Lists file attachments. */
    @Command(name = "list", description = "List file attachments.")
    static final class ListFiles extends CliCommand implements Callable<Integer> {
        @Option(names = "--first", description = "How many to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
        int first;

        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                Connection<SageFile> files = client.files().list(ListOptions.first(first));
                if (files.isEmpty()) {
                    out().println("No files found.");
                    return 0;
                }
                Table table = new Table("ID", "FILE NAME", "ENTITY TYPE", "SIZE");
                for (SageFile f : files) {
                    table.row(f.getId() != null ? f.getId() : "",
                            f.getFileName() != null ? f.getFileName() : "",
                            f.getEntityType() != null ? f.getEntityType() : "",
                            f.getSize() != null ? f.getSize().toString() : "");
                }
                table.print(out());
                if (files.getTotalCount() != null && files.getTotalCount() > files.size()) {
                    out().println(files.size() + " of " + files.getTotalCount() + " shown (--first N for more)");
                }
            }
            return 0;
        }
    }

    /** Uploads a file attachment. */
    @Command(name = "upload", description = "Upload and attach a file to an entity.")
    static final class Upload extends CliCommand implements Callable<Integer> {
        @Option(names = "--entity-type", required = true, description = "CUSTOMER, SUPPLIER, SALES_INVOICE, PURCHASE_INVOICE")
        FileEntityType entityType;

        @Option(names = "--entity-id", required = true, description = "Entity ID (UUID)")
        String entityId;

        @Option(names = "--comment", description = "Optional attachment comment")
        String comment;

        @Parameters(index = "0", description = "File to upload")
        File file;

        @Override
        public Integer call() {
            try (SageActive4jClient client = root().client()) {
                FileAttachment attachment = new FileAttachment()
                        .entityType(entityType)
                        .entityId(entityId)
                        .file(FileUpload.of(file.toPath()));
                if (comment != null) {
                    attachment.comment(comment);
                }
                String fileId = client.files().upload(attachment);
                out().println("Uploaded file successfully: " + fileId);
            }
            return 0;
        }
    }
}
