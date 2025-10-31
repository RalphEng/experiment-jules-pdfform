package com.example.pdfformfiller;

import com.example.pdfformfiller.model.FieldResult;
import com.example.pdfformfiller.model.FieldStatus;
import com.example.pdfformfiller.model.ReportData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {

    public static byte[] loadPdfResource(String filename) {
        try (InputStream is = TestUtils.class.getResourceAsStream("/pdfs/" + filename)) {
            assertThat(is).isNotNull();
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream loadPropertiesResource(String filename) {
        return TestUtils.class.getResourceAsStream("/properties/" + filename);
    }

    public static Map<String, String> readPdfFieldValues(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getDocumentCatalog().getAcroForm().getFields().stream()
                    .collect(Collectors.toMap(PDField::getFullyQualifiedName, TestUtils::getFieldValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFieldValue(PDField field) {
        if (field instanceof PDCheckBox) {
            return ((PDCheckBox) field).isChecked() ? "true" : "false";
        }
        return field.getValueAsString();
    }

    public static Map<String, String> parsePropertiesString(String content) {
        try {
            return PropertyParser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fromOutputStream(ByteArrayOutputStream baos) {
        return baos.toString(StandardCharsets.UTF_8);
    }

    public static ReportData.ReportDataBuilder createBaseReportData() {
        return ReportData.builder()
                .processingTimestamp(Instant.now())
                .fieldResults(List.of())
                .unfilledPdfFields(List.of());
    }

    public static FieldResult createFilledResult(String key, String value, String fieldName, String fieldType) {
        return FieldResult.builder()
                .propertyKey(key)
                .propertyValue(value)
                .pdfFieldName(fieldName)
                .fieldType(fieldType)
                .status(FieldStatus.FILLED)
                .build();
    }
}
