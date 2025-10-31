package com.example.pdfformfiller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

/**
 * Validator to check that PDF forms contain the expected fields.
 */
public class PDFFormValidator {
    
    public static void main(String[] args) {
        // Set headless mode to avoid X11 issues
        System.setProperty("java.awt.headless", "true");
        
        System.out.println("=== PDF Form Validation ===");
        
        // Validate all three PDF forms
        validateSimpleForm();
        validateComplexForm();
        validateSpecialCharsForm();
        
        System.out.println("=== Validation Complete ===");
    }
    
    private static void validateSimpleForm() {
        System.out.println("\n--- Validating sample-form-simple.pdf ---");
        String pdfPath = "src/test/resources/pdfs/sample-form-simple.pdf";
        List<String> expectedFields = Arrays.asList(
            "firstName", "lastName", "email", "phone", "address"
        );
        
        validatePDF(pdfPath, expectedFields);
    }
    
    private static void validateComplexForm() {
        System.out.println("\n--- Validating sample-form-complex.pdf ---");
        String pdfPath = "src/test/resources/pdfs/sample-form-complex.pdf";
        List<String> expectedFields = Arrays.asList(
            "name", "email", "subscribe", "agreeToTerms", "newsletter", 
            "gender", "employmentType", "department", "country", "comments"
        );
        
        validatePDF(pdfPath, expectedFields);
    }
    
    private static void validateSpecialCharsForm() {
        System.out.println("\n--- Validating sample-form-special-chars.pdf ---");
        String pdfPath = "src/test/resources/pdfs/sample-form-special-chars.pdf";
        List<String> expectedFields = Arrays.asList(
            "user.name", "user_email", "address-line1", "field=value", "path\\to\\file"
        );
        
        validatePDF(pdfPath, expectedFields);
    }
    
    private static void validatePDF(String pdfPath, List<String> expectedFields) {
        File pdfFile = new File(pdfPath);
        
        if (!pdfFile.exists()) {
            System.err.println("ERROR: PDF file not found: " + pdfPath);
            return;
        }
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm == null) {
                System.err.println("ERROR: No AcroForm found in " + pdfPath);
                return;
            }
            
            System.out.println("PDF loaded successfully: " + pdfPath);
            System.out.println("Total fields in form: " + acroForm.getFields().size());
            
            // Check each expected field
            boolean allFieldsFound = true;
            for (String fieldName : expectedFields) {
                PDField field = acroForm.getField(fieldName);
                if (field != null) {
                    System.out.println("✓ Field found: " + fieldName + " (Type: " + field.getClass().getSimpleName() + ")");
                } else {
                    System.err.println("✗ Field NOT found: " + fieldName);
                    allFieldsFound = false;
                }
            }
            
            // List all actual fields in the form
            System.out.println("\nAll fields in the PDF:");
            for (PDField field : acroForm.getFields()) {
                System.out.println("  - " + field.getFullyQualifiedName() + " (" + field.getClass().getSimpleName() + ")");
            }
            
            if (allFieldsFound) {
                System.out.println("✓ All expected fields found in " + pdfPath);
            } else {
                System.err.println("✗ Some expected fields missing in " + pdfPath);
            }
            
        } catch (IOException e) {
            System.err.println("ERROR loading PDF " + pdfPath + ": " + e.getMessage());
        }
    }
}