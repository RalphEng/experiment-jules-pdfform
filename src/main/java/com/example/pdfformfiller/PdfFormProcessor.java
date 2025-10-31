package com.example.pdfformfiller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

import com.example.pdfformfiller.model.FieldResult;
import com.example.pdfformfiller.model.FieldStatus;
import com.example.pdfformfiller.model.ReportData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PdfFormProcessor {

    private static final Set<String> CHECKBOX_TRUE_VALUES =
        Set.of("true", "yes", "1", "on");

    public ReportData fillForm(InputStream pdfInput, Map<String, String> properties, OutputStream pdfOutput) throws IOException {
        if (properties == null) {
            throw new IllegalArgumentException("Properties map cannot be null");
        }

        log.info("Starting PDF form processing with {} properties.", properties.size());

        if (properties.isEmpty()) {
            log.warn("Properties map is empty. No fields will be filled.");
        }

        try (PDDocument document = Loader.loadPDF(pdfInput.readAllBytes())) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                throw new IOException("PDF does not contain an AcroForm.");
            }

            List<FieldResult> fieldResults = new ArrayList<>();
            Set<String> filledFieldNames = new HashSet<>();

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                PDField field = acroForm.getField(key);

                FieldResult result;
                if (field == null) {
                    result = FieldResult.builder()
                            .propertyKey(key)
                            .propertyValue(value)
                            .status(FieldStatus.FIELD_NOT_FOUND)
                            .build();
                    log.warn("Property key '{}' does not match any PDF field.", key);
                } else {
                    result = fillField(field, value);
                    filledFieldNames.add(key);
                }
                fieldResults.add(result);
            }

            List<String> unfilledPdfFields = acroForm.getFields().stream()
                    .map(PDField::getFullyQualifiedName)
                    .filter(name -> !filledFieldNames.contains(name))
                    .collect(Collectors.toList());

            acroForm.refreshAppearances();
            document.save(pdfOutput);

            ReportData reportData = ReportData.builder()
                    .totalProperties(properties.size())
                    .fieldResults(fieldResults)
                    .unfilledPdfFields(unfilledPdfFields)
                    .processingTimestamp(Instant.now())
                    .build();
            log.info("PDF form processing complete. Filled: {}, Not Found: {}, Errors: {}",
                    reportData.getFilledCount(), reportData.getNotFoundCount(), reportData.getErrorCount());
            return reportData;
        }
    }

    private FieldResult fillField(PDField field, String value) {
        String fieldName = field.getFullyQualifiedName();
        FieldResult.FieldResultBuilder resultBuilder = FieldResult.builder()
                .propertyKey(fieldName)
                .propertyValue(value)
                .pdfFieldName(fieldName)
                .fieldType(field.getClass().getSimpleName());
        try {
            if (field instanceof PDTextField) {
                field.setValue(value);
            } else if (field instanceof PDCheckBox) {
                fillCheckBox((PDCheckBox) field, value);
            } else if (field instanceof PDRadioButton) {
                fillRadioButton((PDRadioButton) field, value);
            } else if (field instanceof PDChoice) {
                fillChoiceField((PDChoice) field, value);
            } else {
                field.setValue(value);
            }
            log.debug("Filled field '{}' with value '{}'", fieldName, value);
            return resultBuilder.status(FieldStatus.FILLED).build();
        } catch (IOException e) {
            log.error("Error filling field '{}'", fieldName, e);
            return resultBuilder.status(FieldStatus.ERROR).errorMessage(e.getMessage()).build();
        }
    }

    private void fillCheckBox(PDCheckBox checkBox, String value) throws IOException {
        if (CHECKBOX_TRUE_VALUES.contains(value.toLowerCase())) {
            checkBox.check();
        } else {
            checkBox.unCheck();
        }
    }

    private void fillRadioButton(PDRadioButton radioButton, String value) throws IOException {
        if (radioButton.getOnValues().contains(value)) {
            radioButton.setValue(value);
        } else {
            String errorMsg = String.format("Invalid option '%s' for radio button '%s'. Valid options: %s",
                    value, radioButton.getFullyQualifiedName(), radioButton.getOnValues());
            log.warn(errorMsg);
            throw new IOException(errorMsg);
        }
    }

    private void fillChoiceField(PDChoice choiceField, String value) throws IOException {
        if (!choiceField.getOptions().contains(value)) {
            log.warn("Invalid option '{}' for choice field '{}'.", value, choiceField.getFullyQualifiedName());
        }
        choiceField.setValue(value);
    }
}
