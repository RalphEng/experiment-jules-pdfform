# PDFBox 3 - Code-Beispiele für Formular-Operationen

## Basis-Setup

```xml
<!-- pom.xml Dependency -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.6</version>
</dependency>
```

## 1. Formular laden und alle Felder auflisten

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;

public class ListFormFields {
    public static void main(String[] args) {
        try (PDDocument document = Loader.loadPDF(new File("input.pdf"))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                System.out.println("Formular gefunden mit " + acroForm.getFields().size() + " Feldern:");

                for (PDField field : acroForm.getFields()) {
                    String fieldType = field.getFieldType();
                    String fieldName = field.getFullyQualifiedName();
                    String fieldValue = field.getValueAsString();

                    System.out.println("  - " + fieldName);
                    System.out.println("    Typ: " + fieldType);
                    System.out.println("    Wert: " + fieldValue);
                    System.out.println("    Readonly: " + field.isReadOnly());
                    System.out.println("    Required: " + field.isRequired());
                    System.out.println();
                }
            } else {
                System.out.println("Kein Formular in dieser PDF gefunden.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 2. Textfelder ausfüllen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import java.io.File;
import java.io.IOException;

public class FillTextField {
    public static void fillForm(String inputPath, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Einzelnes Feld nach Namen ausfüllen
                PDField field = acroForm.getField("firstName");
                if (field instanceof PDTextField) {
                    field.setValue("Max");
                }

                // Weiteres Feld ausfüllen
                PDField lastNameField = acroForm.getField("lastName");
                if (lastNameField != null) {
                    lastNameField.setValue("Mustermann");
                }

                // Appearance Streams aktualisieren
                acroForm.refreshAppearances();

                // Dokument speichern
                document.save(outputPath);
                System.out.println("Formular erfolgreich ausgefüllt: " + outputPath);
            }
        }
    }

    public static void main(String[] args) {
        try {
            fillForm("template.pdf", "filled.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 3. Checkboxen aktivieren/deaktivieren

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;

public class HandleCheckboxes {
    public static void handleCheckbox(String inputPath, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Checkbox aktivieren
                PDField agreeField = acroForm.getField("agreeToTerms");
                if (agreeField instanceof PDCheckBox) {
                    PDCheckBox checkbox = (PDCheckBox) agreeField;
                    checkbox.check();
                }

                // Checkbox deaktivieren
                PDField declineField = acroForm.getField("decline");
                if (declineField instanceof PDCheckBox) {
                    PDCheckBox checkbox = (PDCheckBox) declineField;
                    checkbox.unCheck();
                }

                // Appearance Streams aktualisieren
                acroForm.refreshAppearances();

                document.save(outputPath);
                System.out.println("Checkboxen gesetzt: " + outputPath);
            }
        }
    }

    public static void main(String[] args) {
        try {
            handleCheckbox("form.pdf", "checked.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 4. Radiobuttons auswählen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class HandleRadioButtons {
    public static void selectRadio(String inputPath, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Radiobutton nach Wert setzen
                PDField genderField = acroForm.getField("gender");
                if (genderField instanceof PDRadioButton) {
                    PDRadioButton radioButton = (PDRadioButton) genderField;

                    // Verfügbare Optionen anzeigen
                    List<String> options = radioButton.getOnValues();
                    System.out.println("Verfügbare Optionen: " + options);

                    // Option auswählen (z.B. "male", "female")
                    radioButton.setValue("male");
                }

                // Appearance Streams aktualisieren
                acroForm.refreshAppearances();

                document.save(outputPath);
                System.out.println("Radiobutton gesetzt: " + outputPath);
            }
        }
    }

    public static void main(String[] args) {
        try {
            selectRadio("form.pdf", "selected.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 5. Dropdown-Listen (ComboBox) ausfüllen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class HandleComboBox {
    public static void selectComboBox(String inputPath, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                PDField countryField = acroForm.getField("country");
                if (countryField instanceof PDComboBox) {
                    PDComboBox comboBox = (PDComboBox) countryField;

                    // Verfügbare Optionen anzeigen
                    List<String> options = comboBox.getOptions();
                    System.out.println("Verfügbare Länder: " + options);

                    // Option auswählen
                    comboBox.setValue("Germany");
                }

                // Appearance Streams aktualisieren
                acroForm.refreshAppearances();

                document.save(outputPath);
                System.out.println("ComboBox gesetzt: " + outputPath);
            }
        }
    }

    public static void main(String[] args) {
        try {
            selectComboBox("form.pdf", "selected.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 6. Formular aus Properties-Datei ausfüllen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FillFromProperties {
    public static void fillFormFromProperties(
            String pdfPath,
            String propertiesPath,
            String outputPath) throws IOException {

        // Properties laden
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            props.load(fis);
        }

        // PDF laden und Formular ausfüllen
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Durch alle Properties iterieren
                for (String fieldName : props.stringPropertyNames()) {
                    String fieldValue = props.getProperty(fieldName);

                    PDField field = acroForm.getField(fieldName);
                    if (field != null) {
                        // Checkbox-spezielle Behandlung
                        if (field instanceof PDCheckBox) {
                            PDCheckBox checkbox = (PDCheckBox) field;
                            if ("true".equalsIgnoreCase(fieldValue) || "yes".equalsIgnoreCase(fieldValue)) {
                                checkbox.check();
                            } else {
                                checkbox.unCheck();
                            }
                        } else {
                            // Normale Felder (Text, Radio, etc.)
                            field.setValue(fieldValue);
                        }

                        System.out.println("Feld gesetzt: " + fieldName + " = " + fieldValue);
                    } else {
                        System.out.println("Warnung: Feld nicht gefunden: " + fieldName);
                    }
                }

                // Appearance Streams aktualisieren
                acroForm.refreshAppearances();

                // Dokument speichern
                document.save(outputPath);
                System.out.println("Formular erfolgreich ausgefüllt: " + outputPath);
            }
        }
    }

    public static void main(String[] args) {
        try {
            fillFormFromProperties(
                "template.pdf",
                "data.properties",
                "filled_output.pdf"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

**Beispiel data.properties:**
```properties
firstName=Max
lastName=Mustermann
email=max.mustermann@example.com
agreeToTerms=true
country=Germany
gender=male
```

## 7. Formular flattenen (nicht mehr editierbar machen)

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import java.io.File;
import java.io.IOException;

public class FlattenForm {
    public static void flattenForm(String inputPath, String outputPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(inputPath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Appearance Streams vor dem Flattening aktualisieren
                acroForm.refreshAppearances();

                // Formular flattenen (in statischen Content umwandeln)
                acroForm.flatten();

                System.out.println("Formular geflattent (nicht mehr editierbar)");
            }

            document.save(outputPath);
            System.out.println("Gespeichert: " + outputPath);
        }
    }

    public static void main(String[] args) {
        try {
            flattenForm("filled.pdf", "flattened.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 8. Verschachteltes Feld auslesen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;

public class ReadNestedFields {
    public static void readAllFields(PDField parentField, String prefix) throws IOException {
        String fieldName = parentField.getPartialName();
        String fullName = parentField.getFullyQualifiedName();
        String value = parentField.getValueAsString();

        System.out.println(prefix + fullName + " = " + value);

        // Wenn das Feld Kinder hat, rekursiv durchlaufen
        for (PDField child : parentField.getChildren()) {
            readAllFields(child, prefix + "  ");
        }
    }

    public static void main(String[] args) {
        try (PDDocument document = Loader.loadPDF(new File("form.pdf"))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                System.out.println("Alle Formularfelder (inkl. verschachtelte):");
                for (PDField field : acroForm.getFields()) {
                    readAllFields(field, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 9. Feld-Validierung durchführen

```java
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import java.io.IOException;

public class ValidateField {
    public static boolean validateField(PDField field) throws IOException {
        String fieldName = field.getFullyQualifiedName();
        String value = field.getValueAsString();

        // Prüfen ob Pflichtfeld
        if (field.isRequired() && (value == null || value.trim().isEmpty())) {
            System.err.println("Fehler: Pflichtfeld '" + fieldName + "' ist leer");
            return false;
        }

        // Prüfen ob Readonly (sollte nicht geändert werden)
        if (field.isReadOnly()) {
            System.out.println("Info: Feld '" + fieldName + "' ist schreibgeschützt");
        }

        // Spezielle Validierung für Textfelder
        if (field instanceof PDTextField) {
            PDTextField textField = (PDTextField) field;
            int maxLen = textField.getMaxLen();

            if (maxLen > 0 && value != null && value.length() > maxLen) {
                System.err.println("Fehler: Feld '" + fieldName + "' überschreitet Maximallänge von " + maxLen);
                return false;
            }
        }

        return true;
    }
}
```

## 10. Komplettes Beispiel: CLI-Tool zum Ausfüllen

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PdfFormFillerCli {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Verwendung: java PdfFormFillerCli <template.pdf> <data.properties> <output.pdf>");
            System.exit(1);
        }

        String templatePath = args[0];
        String dataPath = args[1];
        String outputPath = args[2];

        try {
            fillPdfForm(templatePath, dataPath, outputPath);
            System.out.println("✓ Formular erfolgreich ausgefüllt: " + outputPath);
        } catch (Exception e) {
            System.err.println("✗ Fehler beim Ausfüllen des Formulars: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void fillPdfForm(String templatePath, String dataPath, String outputPath)
            throws IOException {

        // Properties laden
        Properties data = new Properties();
        try (FileInputStream fis = new FileInputStream(dataPath)) {
            data.load(fis);
        }

        // PDF laden
        try (PDDocument document = Loader.loadPDF(new File(templatePath))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm == null) {
                throw new IOException("PDF enthält kein ausfüllbares Formular");
            }

            int filledCount = 0;
            int errorCount = 0;

            // Felder ausfüllen
            for (String fieldName : data.stringPropertyNames()) {
                String fieldValue = data.getProperty(fieldName);
                PDField field = acroForm.getField(fieldName);

                if (field == null) {
                    System.err.println("⚠ Warnung: Feld '" + fieldName + "' nicht gefunden");
                    errorCount++;
                    continue;
                }

                try {
                    if (field instanceof PDCheckBox) {
                        PDCheckBox checkbox = (PDCheckBox) field;
                        if ("true".equalsIgnoreCase(fieldValue) || "yes".equalsIgnoreCase(fieldValue)) {
                            checkbox.check();
                        } else {
                            checkbox.unCheck();
                        }
                    } else {
                        field.setValue(fieldValue);
                    }
                    filledCount++;
                    System.out.println("✓ " + fieldName + " = " + fieldValue);
                } catch (IOException e) {
                    System.err.println("✗ Fehler bei Feld '" + fieldName + "': " + e.getMessage());
                    errorCount++;
                }
            }

            // Appearance Streams aktualisieren
            acroForm.refreshAppearances();

            // Speichern
            document.save(outputPath);

            System.out.println("\nZusammenfassung:");
            System.out.println("  Erfolgreich ausgefüllt: " + filledCount);
            System.out.println("  Fehler: " + errorCount);
        }
    }
}
```

---

## Wichtige Hinweise

1. **Immer try-with-resources verwenden** für `PDDocument`
2. **`acroForm.refreshAppearances()` aufrufen** nach dem Ändern von Feldwerten
3. **Null-Checks durchführen** bevor auf Felder zugegriffen wird
4. **Vollqualifizierte Namen** für verschachtelte Felder verwenden
5. **Feldtyp prüfen** vor dem Casting (`instanceof`)

---

**Apache PDFBox Version:** 3.0.6
**Java Version:** 17+
