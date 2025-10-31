# Apache PDFBox 3 - Formular-Dokumentation

Diese Dokumentation enthält wichtige Informationen zur Arbeit mit PDF-Formularen in Apache PDFBox 3.0.6.

## Inhaltsverzeichnis
- [Formularfelder und AcroForm](#formularfelder-und-acroform)
- [Dokument laden](#dokument-laden)
- [Formularfelder ausfüllen](#formularfelder-ausfüllen)
- [Formular-Flattening](#formular-flattening)
- [Signaturfelder](#signaturfelder)
- [Button-Felder](#button-felder)
- [Weitere Ressourcen](#weitere-ressourcen)

---

## Formularfelder und AcroForm

### Field Appearance für neue Werte erstellen

Wenn der `NeedAppearances`-Flag auf true gesetzt ist, müssen Appearance-Streams für Formularfelder generiert werden, um die Werte korrekt anzuzeigen.

```java
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDAppearance;

// Feld-Objekt abrufen und Wert setzen
PDField field = // ... Feld-Objekt abrufen ...
field.setValue("Neuer Wert");

// Wenn NeedAppearances true ist, sicherstellen, dass Appearance-Streams generiert/aktualisiert werden
if (field.getAcroForm().isNeedAppearances()) {
    PDAppearance appearance = field.getDefaultAppearance();
    // Code zum Generieren oder Aktualisieren des Appearance-Streams basierend auf dem neuen Wert
}
```

**Wichtig:** Dies ist essenziell, um sicherzustellen, dass Formularfelder ihre Werte nach der Änderung korrekt anzeigen, besonders bei interaktiven Formularen.

### Formularfeld-Typen

PDFBox unterstützt verschiedene Formularfeld-Typen:
- `PDTextField` - Textfelder
- `PDCheckBox` - Checkboxen
- `PDRadioButton` - Radiobuttons
- `PDComboBox` - Dropdown-Listen
- `PDListBox` - Listenboxen

---

## Dokument laden

### PDF-Dokument laden

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.IOException;

public class LoadPDFExample {
    public static void main(String[] args) {
        try (PDDocument document = Loader.loadPDF(new File("input.pdf"))) {
            System.out.println("Seiten: " + document.getNumberOfPages());
            System.out.println("Titel: " + document.getDocumentInformation().getTitle());
            System.out.println("Autor: " + document.getDocumentInformation().getAuthor());
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der PDF: " + e.getMessage());
        }
    }
}
```

---

## Formularfelder ausfüllen

### Grundlegendes Beispiel zum Ausfüllen von Feldern

```java
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import java.io.File;
import java.io.IOException;

public class FillFormExample {
    public static void main(String[] args) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File("form.pdf"))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                // Einzelnes Feld nach Namen abrufen und ausfüllen
                PDField field = acroForm.getField("fieldName");
                if (field != null) {
                    field.setValue("Neuer Wert");
                }

                // Alle Felder durchlaufen
                for (PDField formField : acroForm.getFields()) {
                    String fieldName = formField.getFullyQualifiedName();
                    System.out.println("Feld: " + fieldName);
                }
            }

            document.save("filled_form.pdf");
        }
    }
}
```

### NeedAppearances Flag

Wenn `NeedAppearances` auf `true` gesetzt ist, generiert der PDF-Viewer die Darstellung der Formularfelder automatisch. Dies sollte idealerweise auf `false` gesetzt werden, nachdem eigene Appearance-Streams generiert wurden:

```java
if (acroForm != null) {
    acroForm.setNeedAppearances(false);
}
```

---

## Formular-Flattening

### AcroForm-Flattening

Beim Flattening werden interaktive Formularfelder in statischen Content umgewandelt. Dabei müssen versteckte Felder berücksichtigt werden.

```java
public void flattenAcroForm(PDDocument document) throws IOException {
    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

    if (acroForm != null) {
        // Vor dem Flattening sicherstellen, dass alle Felder korrekt dargestellt werden
        acroForm.refreshAppearances();

        // Formular flattenen
        acroForm.flatten();
    }

    document.save("flattened_form.pdf");
}
```

**Hinweis:** Bei PDFBox 3 wurde ein Bug behoben, bei dem versteckte Felder beim Flattening sichtbar wurden. Der Flattening-Prozess respektiert jetzt den Sichtbarkeitsstatus der Formularfelder.

---

## Signaturfelder

### Signaturfelder abrufen

```java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import java.util.List;

public List<PDSignatureField> getAllSignatureFields(PDDocument document) {
    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
    List<PDSignatureField> signatureFields = new ArrayList<>();

    if (acroForm != null) {
        for (PDField field : acroForm.getFields()) {
            if (field instanceof PDSignatureField) {
                signatureFields.add((PDSignatureField) field);
            }
        }
    }

    return signatureFields;
}
```

**Wichtig:** In PDFBox 3 wurde ein Bug behoben, bei dem `getSignatureFields()` nur Top-Level-Felder zurückgab. Jetzt werden auch verschachtelte Signaturfelder innerhalb von Gruppen oder Formularstrukturen korrekt abgerufen.

---

## Button-Felder

### PDButton On-Values

```java
import org.apache.pdfbox.pdmodel.interactive.form.PDButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;

public void handleButtonValues(PDButton button) {
    // Erlaubte Werte für Checkboxen und Radiobuttons abrufen
    List<String> onValues = button.getOnValues();

    // Checkbox aktivieren
    if (button instanceof PDCheckBox) {
        PDCheckBox checkBox = (PDCheckBox) button;
        checkBox.check(); // Aktiviert die Checkbox
        // oder: checkBox.unCheck(); zum Deaktivieren
    }

    // Radiobutton aktivieren
    if (button instanceof PDRadioButton) {
        PDRadioButton radioButton = (PDRadioButton) button;
        if (!onValues.isEmpty()) {
            radioButton.setValue(onValues.get(0));
        }
    }
}
```

**Hinweis:** In PDFBox 3 wurde ein Bug in `PDButton.getOnValues()` behoben, der falsche Datenquellen verwendete.

---

## Weitere Ressourcen

### Dokument-Metadaten

```java
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import java.util.Calendar;

public void setMetadata(PDDocument document) {
    PDDocumentInformation info = document.getDocumentInformation();

    info.setTitle("Ausgefülltes Formular");
    info.setAuthor("PDF Form Filler");
    info.setSubject("Automatisch ausgefülltes Formular");
    info.setKeywords("PDF, Formular, Automatisch");
    info.setModificationDate(Calendar.getInstance());
}
```

### PDF verschlüsseln

```java
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public void encryptPdf(PDDocument document) throws IOException {
    AccessPermission permission = new AccessPermission();
    permission.setCanPrint(true);
    permission.setCanExtractContent(false);
    permission.setCanModify(false);
    permission.setCanModifyAnnotations(false);

    String ownerPassword = "owner123";
    String userPassword = "user123";

    StandardProtectionPolicy policy = new StandardProtectionPolicy(
        ownerPassword,
        userPassword,
        permission
    );
    policy.setEncryptionKeyLength(256); // AES-256 Verschlüsselung

    document.protect(policy);
    document.save("encrypted.pdf");
}
```

### Verschlüsseltes Dokument laden

```java
try (PDDocument protectedDoc = Loader.loadPDF(new File("encrypted.pdf"), "user123")) {
    System.out.println("Verschlüsseltes Dokument erfolgreich geladen");
    System.out.println("Drucken erlaubt: " +
                      protectedDoc.getCurrentAccessPermission().canPrint());
}
```

---

## Zusammenfassung wichtiger Klassen

- **PDDocument** - Haupt-Dokument-Klasse
- **Loader** - Zum Laden von PDF-Dokumenten
- **PDAcroForm** - Repräsentiert das AcroForm (Formular) im Dokument
- **PDField** - Basis-Klasse für alle Formularfelder
- **PDTextField** - Textfeld
- **PDCheckBox** - Checkbox
- **PDRadioButton** - Radiobutton
- **PDComboBox** - Dropdown-Liste
- **PDListBox** - Listenbox
- **PDButton** - Basis-Klasse für Buttons
- **PDSignatureField** - Signaturfeld

---

## Best Practices

1. **Try-with-resources verwenden**: Immer `PDDocument` in einem try-with-resources-Block verwenden, um sicherzustellen, dass Ressourcen freigegeben werden.

2. **NeedAppearances korrekt handhaben**: Wenn Formularfelder programmgesteuert ausgefüllt werden, sollte `NeedAppearances` auf `false` gesetzt werden, nachdem eigene Appearance-Streams generiert wurden.

3. **Felder prüfen**: Vor dem Zugriff auf Felder immer prüfen, ob `acroForm` und das Feld nicht `null` sind.

4. **Vollqualifizierte Namen verwenden**: Für verschachtelte Felder den vollqualifizierten Namen mit `getFullyQualifiedName()` verwenden.

5. **Appearance Streams aktualisieren**: Nach dem Ändern von Feldwerten `acroForm.refreshAppearances()` aufrufen oder manuelle Appearance-Streams generieren.

---

**Version:** Apache PDFBox 3.0.6
**Letzte Aktualisierung:** 2025-10-31
