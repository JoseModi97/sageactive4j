package io.github.josemodi97.sageactive4j.cli;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.FileUpload;
import io.github.josemodi97.sageactive4j.input.FileAttachment;
import io.github.josemodi97.sageactive4j.input.FileEntityType;
import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Uploads a document to Sage Active's OCR pipeline (AP Automation). */
@Command(name = "ocr", description = "Upload a receipt or invoice document for automated OCR processing (AP Automation).")
final class OcrCommand extends CliCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Document file to process (PDF, PNG, JPG)")
    File file;

    @Option(names = "--comment", description = "Optional comment or invoice reference")
    String comment;

    @Override
    public Integer call() {
        if (!file.exists()) {
            throw new CliException("File not found: " + file.getAbsolutePath());
        }
        try (SageActive4jClient client = root().client()) {
            FileAttachment attachment = new FileAttachment()
                    .entityType(FileEntityType.AP_AUTOMATION)
                    .file(FileUpload.of(file.toPath()));
            if (comment != null) {
                attachment.comment(comment);
            }
            String fileId = client.files().upload(attachment);
            out().println("Uploaded document to Sage Active OCR engine (AP Automation).");
            out().println("File ID: " + fileId);
        }
        return 0;
    }
}
