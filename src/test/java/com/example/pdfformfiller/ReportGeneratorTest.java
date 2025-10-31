package com.example.pdfformfiller;

import com.example.pdfformfiller.model.FieldResult;
import com.example.pdfformfiller.model.FieldStatus;
import com.example.pdfformfiller.model.ReportData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportGeneratorTest {

    private String generateReport(ReportData reportData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ReportGenerator().generate(reportData, baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    private ReportData.ReportDataBuilder createBaseReportData() {
        return ReportData.builder()
                .processingTimestamp(Instant.parse("2023-01-01T12:00:00Z"))
                .fieldResults(Collections.emptyList())
                .unfilledPdfFields(Collections.emptyList());
    }

    @Nested
    class ReportStructure {
        @Test
        void testReportHasHeaderAndFooter() throws IOException {
            ReportData reportData = createBaseReportData().build();
            String report = generateReport(reportData);
            assertThat(report).startsWith("========================================");
            assertThat(report).endsWith("========================================\n");
            assertThat(report).contains("PDF Form Processing Report");
            assertThat(report).contains("End of Report");
        }

        @Test
        void testReportHasTimestamp() throws IOException {
            ReportData reportData = createBaseReportData().build();
            String report = generateReport(reportData);
            assertThat(report).contains("Processing Time: 2023-01-01T12:00:00Z");
        }

        @Test
        void testReportHasSummarySection() throws IOException {
            ReportData reportData = createBaseReportData().totalProperties(5).build();
            String report = generateReport(reportData);
            assertThat(report).contains("SUMMARY");
            assertThat(report).contains("Total Properties: 5");
        }
    }

    @Nested
    class FilledFieldsSection {
        @Test
        void testFilledFieldsPresent() throws IOException {
            FieldResult filled = FieldResult.builder()
                    .status(FieldStatus.FILLED)
                    .propertyKey("key")
                    .pdfFieldName("key")
                    .fieldType("PDTextField")
                    .propertyValue("value")
                    .build();
            ReportData reportData = createBaseReportData().fieldResults(List.of(filled)).build();
            String report = generateReport(reportData);
            assertThat(report).contains("FILLED FIELDS");
            assertThat(report).contains("key -> key (PDTextField)");
            assertThat(report).contains("Value: value");
        }

        @Test
        void testFilledFieldsAbsent() throws IOException {
            ReportData reportData = createBaseReportData().build();
            String report = generateReport(reportData);
            assertThat(report).doesNotContain("FILLED FIELDS");
        }
    }
}
