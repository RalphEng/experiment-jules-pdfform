package com.example.pdfformfiller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.example.pdfformfiller.model.FieldResult;
import com.example.pdfformfiller.model.ReportData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReportGenerator {

    public void generate(ReportData reportData, OutputStream output) throws IOException {
        log.debug("Starting report generation.");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));

        writeHeader(writer, reportData);
        writeSummary(writer, reportData);
        writeFilledFields(writer, reportData);
        writeNotFoundFields(writer, reportData);
        writeErrors(writer, reportData);
        writeUnfilledPdfFields(writer, reportData);
        writeFooter(writer);

        writer.flush();
        log.info("Report generation complete.");
    }

    private void writeHeader(BufferedWriter writer, ReportData reportData) throws IOException {
        writer.write("========================================\n");
        writer.write("PDF Form Processing Report\n");
        writer.write("========================================\n");
        writer.write("Processing Time: " + DateTimeFormatter.ISO_INSTANT.format(reportData.getProcessingTimestamp()) + "\n\n");
    }

    private void writeSummary(BufferedWriter writer, ReportData reportData) throws IOException {
        writer.write("SUMMARY\n");
        writer.write("-------\n");
        writer.write("Total Properties: " + reportData.getTotalProperties() + "\n");
        writer.write("Successfully Filled: " + reportData.getFilledCount() + "\n");
        writer.write("Fields Not Found: " + reportData.getNotFoundCount() + "\n");
        writer.write("Errors: " + reportData.getErrorCount() + "\n");
        writer.write("Unfilled PDF Fields: " + reportData.getUnfilledPdfFields().size() + "\n\n");
    }

    private void writeFilledFields(BufferedWriter writer, ReportData reportData) throws IOException {
        List<FieldResult> filled = reportData.getFieldResults().stream()
                .filter(r -> r.getStatus() == com.example.pdfformfiller.model.FieldStatus.FILLED)
                .collect(Collectors.toList());
        if (!filled.isEmpty()) {
            writer.write("FILLED FIELDS\n");
            writer.write("-------------\n");
            for (FieldResult result : filled) {
                writer.write(String.format("%s -> %s (%s)\n", result.getPropertyKey(), result.getPdfFieldName(), result.getFieldType()));
                writer.write("  Value: " + truncateValue(result.getPropertyValue(), 100) + "\n");
            }
            writer.write("\n");
        }
    }

    private void writeNotFoundFields(BufferedWriter writer, ReportData reportData) throws IOException {
        List<FieldResult> notFound = reportData.getFieldResults().stream()
                .filter(r -> r.getStatus() == com.example.pdfformfiller.model.FieldStatus.FIELD_NOT_FOUND)
                .collect(Collectors.toList());
        if (!notFound.isEmpty()) {
            writer.write("FIELDS NOT FOUND IN PDF\n");
            writer.write("-----------------------\n");
            for (FieldResult result : notFound) {
                writer.write(result.getPropertyKey() + "\n");
                writer.write("  Value: " + truncateValue(result.getPropertyValue(), 100) + "\n");
            }
            writer.write("\n");
        }
    }

    private void writeErrors(BufferedWriter writer, ReportData reportData) throws IOException {
        List<FieldResult> errors = reportData.getFieldResults().stream()
                .filter(r -> r.getStatus() == com.example.pdfformfiller.model.FieldStatus.ERROR)
                .collect(Collectors.toList());
        if (!errors.isEmpty()) {
            writer.write("ERRORS\n");
            writer.write("------\n");
            for (FieldResult result : errors) {
                writer.write(String.format("%s -> %s\n", result.getPropertyKey(), result.getPdfFieldName()));
                writer.write("  Value: " + truncateValue(result.getPropertyValue(), 100) + "\n");
                writer.write("  Error: " + result.getErrorMessage() + "\n");
            }
            writer.write("\n");
        }
    }

    private void writeUnfilledPdfFields(BufferedWriter writer, ReportData reportData) throws IOException {
        if (!reportData.getUnfilledPdfFields().isEmpty()) {
            writer.write("UNFILLED PDF FIELDS\n");
            writer.write("-------------------\n");
            for (String fieldName : reportData.getUnfilledPdfFields()) {
                writer.write(fieldName + "\n");
            }
            writer.write("\n");
        }
    }

    private void writeFooter(BufferedWriter writer) throws IOException {
        writer.write("========================================\n");
        writer.write("End of Report\n");
        writer.write("========================================\n");
    }

    private String truncateValue(String value, int maxLength) {
        if (value.length() > maxLength) {
            return value.substring(0, maxLength - 3) + "...";
        }
        return value;
    }
}
