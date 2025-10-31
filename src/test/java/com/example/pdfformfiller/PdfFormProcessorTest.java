package com.example.pdfformfiller;

import com.example.pdfformfiller.model.ReportData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PdfFormProcessorTest {

    private byte[] loadPdfResource(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/pdfs/" + filename)) {
            assertThat(is).isNotNull();
            return is.readAllBytes();
        }
    }

    private Map<String, String> verifyFilledPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getDocumentCatalog().getAcroForm().getFields().stream()
                    .collect(java.util.stream.Collectors.toMap(PDField::getFullyQualifiedName, this::getFieldValue));
        }
    }

    private String getFieldValue(PDField field) {
        if (field instanceof PDCheckBox) {
            return ((PDCheckBox) field).isChecked() ? "true" : "false";
        }
        return field.getValueAsString();
    }

    @Nested
    class SimpleFormTests {
        @Test
        void testFillAllTextFields() throws IOException {
            byte[] pdf = loadPdfResource("sample-form-simple.pdf");
            Map<String, String> properties = Map.of(
                    "firstName", "Max",
                    "lastName", "Mustermann",
                    "email", "max@example.com",
                    "phone", "+49123456789",
                    "address", "Musterstraße 123\n12345 Musterstadt"
            );
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ReportData report = new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output);

            assertThat(report.getFilledCount()).isEqualTo(5);
            assertThat(report.getNotFoundCount()).isZero();
            assertThat(report.getErrorCount()).isZero();
            assertThat(report.getUnfilledPdfFields()).isEmpty();

            Map<String, String> filledValues = verifyFilledPdf(output.toByteArray());
            assertThat(filledValues).containsAllEntriesOf(properties);
        }

        @Test
        void testFieldNotFound() throws IOException {
            byte[] pdf = loadPdfResource("sample-form-simple.pdf");
            Map<String, String> properties = Map.of("nonexistent", "value");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ReportData report = new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output);

            assertThat(report.getFilledCount()).isZero();
            assertThat(report.getNotFoundCount()).isEqualTo(1);
            assertThat(report.getErrorCount()).isZero();
        }
    }

    @Nested
    class ComplexFormTests {
        @Test
        void testFillCheckboxes() throws IOException {
            byte[] pdf = loadPdfResource("sample-form-complex.pdf");
            Map<String, String> properties = Map.of(
                    "subscribe", "true",
                    "agreeToTerms", "false",
                    "newsletter", "yes"
            );
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output);

            Map<String, String> filledValues = verifyFilledPdf(output.toByteArray());
            assertThat(filledValues.get("subscribe")).isEqualTo("true");
            assertThat(filledValues.get("agreeToTerms")).isEqualTo("false");
            assertThat(filledValues.get("newsletter")).isEqualTo("true");
        }

        @Test
        void testRadioButtonSelection() throws IOException {
            byte[] pdf = loadPdfResource("sample-form-complex.pdf");
            Map<String, String> properties = Map.of("gender", "Male");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output);

            Map<String, String> filledValues = verifyFilledPdf(output.toByteArray());
            assertThat(filledValues.get("gender")).isEqualTo("Male");
        }

        @Test
        void testDropdownSelection() throws IOException {
            byte[] pdf = loadPdfResource("sample-form-complex.pdf");
            Map<String, String> properties = Map.of("department", "Engineering");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output);

            Map<String, String> filledValues = verifyFilledPdf(output.toByteArray());
            assertThat(filledValues.get("department")).isEqualTo("Engineering");
        }
    }

    @Nested
    class ErrorHandling {
        @Test
        void testInvalidPdfInput() {
            byte[] pdf = "not-a-pdf".getBytes();
            Map<String, String> properties = Map.of();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            assertThatThrownBy(() -> new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output))
                    .isInstanceOf(IOException.class);
        }

        @Test
        void testPdfWithoutAcroForm() throws IOException {
            // Create a dummy PDF without a form
            try (PDDocument doc = new PDDocument()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                doc.save(baos);
                byte[] pdf = baos.toByteArray();
                Map<String, String> properties = Map.of();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                assertThatThrownBy(() -> new PdfFormProcessor().fillForm(new ByteArrayInputStream(pdf), properties, output))
                        .isInstanceOf(IOException.class)
                        .hasMessage("PDF does not contain an AcroForm.");
            }
        }
    }
}
