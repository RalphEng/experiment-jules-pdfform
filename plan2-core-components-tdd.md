I have created the following plan after thorough exploration and analysis of the codebase. Follow the below plan verbatim. Trust the files and references. Do not re-verify what's written in the plan. Explore only when absolutely necessary. First implement all the proposed file changes and then I'll review all the changes together at the end.

### Observations

The project structure is complete with Maven dependencies (PDFBox 3.0.6, Picocli, SLF4J/Logback, Lombok, JUnit 5, AssertJ), three validated test PDFs with documented fields, and logging configurations. The test PDFs contain various field types: text fields (including multiline), checkboxes with export values "Yes"/"Off", radio button groups, and dropdowns. The special-chars PDF tests edge cases with dots, underscores, hyphens, equals signs, and backslashes in field names. The task requires implementing stream-based APIs with TDD approach, custom property parsing with escape sequences (`\=` → `=`, `\\` → `\`), and comprehensive logging using Lombok's @Slf4j.

### Approach

Implement three core components using Test-Driven Development with stream-based architecture: (1) PropertyParser for parsing properties with custom escape sequences, (2) PdfFormProcessor for filling PDF forms from property maps, and (3) ReportGenerator for creating text reports. Start by designing data models (ReportData, FieldResult), then write comprehensive tests for each component covering happy paths, edge cases, and field type variations. Implement each component to pass tests using real PDFBox operations on in-memory streams. Use Lombok annotations (@Slf4j, @Data, @Builder) and comprehensive logging throughout.

### Reasoning

I explored the project structure and found complete Maven setup with all dependencies, three test PDFs with documented fields (simple with 5 text fields, complex with 10 mixed-type fields including checkboxes/radios/dropdowns, special-chars with 5 fields testing edge cases), PDFBox documentation showing stream support (Loader.loadPDF(InputStream) and doc.save(OutputStream)), and researched Java Properties escape handling, PDFBox checkbox/radio value handling, and stream-based testing best practices with JUnit 5.

## Mermaid Diagram

sequenceDiagram
    participant Test as Test Suite
    participant PP as PropertyParser
    participant PFP as PdfFormProcessor
    participant RG as ReportGenerator
    participant PDF as PDFBox Library
    
    Note over Test: TDD Approach: Write Tests First
    
    rect rgb(240, 248, 255)
        Note over Test,PP: Phase 1: Property Parsing
        Test->>PP: parse(InputStream)
        PP->>PP: Read lines with UTF-8
        PP->>PP: Handle line continuation
        PP->>PP: Find unescaped separator
        PP->>PP: Unescape key & value<br/>(\\= → =, \\\\ → \)
        PP-->>Test: Map<String, String>
        Test->>Test: Assert properties parsed correctly
    end
    
    rect rgb(255, 248, 240)
        Note over Test,PFP: Phase 2: PDF Form Filling
        Test->>PFP: fillForm(InputStream, Map, OutputStream)
        PFP->>PDF: Loader.loadPDF(InputStream)
        PDF-->>PFP: PDDocument
        PFP->>PDF: getAcroForm()
        PDF-->>PFP: PDAcroForm
        
        loop For each property
            PFP->>PDF: getField(propertyKey)
            alt Field exists
                PFP->>PFP: Determine field type<br/>(Text/Checkbox/Radio/Choice)
                PFP->>PDF: setValue() or check()/unCheck()
                PFP->>PFP: Create FieldResult(FILLED)
            else Field not found
                PFP->>PFP: Create FieldResult(FIELD_NOT_FOUND)
            end
        end
        
        PFP->>PDF: refreshAppearances()
        PFP->>PDF: save(OutputStream)
        PFP->>PFP: Build ReportData
        PFP-->>Test: ReportData
        
        Test->>PDF: Load filled PDF from output
        Test->>PDF: Read field values
        Test->>Test: Assert fields filled correctly
    end
    
    rect rgb(240, 255, 240)
        Note over Test,RG: Phase 3: Report Generation
        Test->>RG: generate(ReportData, OutputStream)
        RG->>RG: Write header & timestamp
        RG->>RG: Write summary section
        RG->>RG: Write filled fields section
        RG->>RG: Write not found section
        RG->>RG: Write errors section
        RG->>RG: Write unfilled fields section
        RG->>RG: Write footer
        RG-->>Test: (void - data in OutputStream)
        Test->>Test: Assert report format correct
    end

## Proposed File Changes

### src/main/java/com/example/pdfformfiller/model/FieldResult.java(NEW)

Create immutable data class representing the result of processing a single form field:

**Purpose**: Capture the outcome of attempting to fill a PDF form field with a property value.

**Fields** (use Lombok @Data, @Builder, @AllArgsConstructor):
- `String propertyKey` - the property key from the properties file
- `String propertyValue` - the value from the properties file
- `String pdfFieldName` - the PDF field name (may be same as propertyKey or null if not found)
- `FieldStatus status` - enum indicating the result (FILLED, FIELD_NOT_FOUND, ERROR)
- `String fieldType` - the PDFBox field type (e.g., "PDTextField", "PDCheckBox") or null if field not found
- `String errorMessage` - error details if status is ERROR, null otherwise

**Annotations**:
- @Data - generates getters, equals, hashCode, toString
- @Builder - enables builder pattern for test data creation
- @AllArgsConstructor - constructor with all fields

**Notes**:
- This class is used by PdfFormProcessor to track each field processing attempt
- Tests will create instances using the builder pattern
- The status enum helps distinguish between different failure modes

### src/main/java/com/example/pdfformfiller/model/FieldStatus.java(NEW)

Create enum representing the status of a field processing operation:

**Purpose**: Categorize the outcome of attempting to fill a PDF form field.

**Enum Values**:
- `FILLED` - field was successfully filled with the property value
- `FIELD_NOT_FOUND` - property key does not match any PDF form field
- `ERROR` - an error occurred during field filling (e.g., IOException, invalid value for field type)

**Notes**:
- Used by FieldResult to indicate processing status
- Helps distinguish between "field doesn't exist" vs "field exists but couldn't be filled"
- Report generation uses these statuses to categorize results

### src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)

Create immutable data class containing all information needed to generate a processing report:

**Purpose**: Aggregate results from PDF form processing for report generation.

**Fields** (use Lombok @Data, @Builder, @AllArgsConstructor):
- `int totalProperties` - total number of properties in the input
- `List<FieldResult> fieldResults` - detailed results for each property/field combination
- `List<String> unfilledPdfFields` - PDF fields that exist but were not filled (no matching property)
- `Instant processingTimestamp` - when the processing occurred

**Derived Counts** (add getter methods, not fields):
- `int getFilledCount()` - count of fieldResults with status FILLED
- `int getNotFoundCount()` - count of fieldResults with status FIELD_NOT_FOUND
- `int getErrorCount()` - count of fieldResults with status ERROR

**Annotations**:
- @Data - generates getters, equals, hashCode, toString
- @Builder - enables builder pattern for test data creation
- @AllArgsConstructor - constructor with all fields

**Notes**:
- This is the contract between PdfFormProcessor and ReportGenerator
- Tests will create instances with various combinations of results
- The unfilledPdfFields list helps identify PDF fields that weren't mapped to properties
- Import `java.time.Instant` for timestamp
- Import `java.util.List` for collections

### src/main/java/com/example/pdfformfiller/model/package-info.java(NEW)

Create package documentation:

**Content**:
```
/**
 * Data model classes for PDF form processing.
 * 
 * Contains immutable data transfer objects used to communicate
 * processing results between core components.
 */
package com.example.pdfformfiller.model;
```

### src/main/java/com/example/pdfformfiller/PropertyParser.java(NEW)

References: 

- lib-doc/pdfbox/code-examples.md

Create class for parsing properties files with custom escape sequences:

**Purpose**: Parse properties from an InputStream with support for custom escape sequences `\=` → `=` and `\\` → `\`.

**Class Structure** (use Lombok @Slf4j for logging):
- Public static method: `Map<String, String> parse(InputStream input) throws IOException`
- Private helper methods for parsing logic

**Parsing Algorithm**:
1. Read input stream with BufferedReader using UTF-8 encoding (InputStreamReader with StandardCharsets.UTF_8)
2. For each line:
   - Skip empty lines and comments (lines starting with # or !)
   - Handle line continuation: if line ends with odd number of backslashes, join with next line
   - Find first unescaped separator (=, :, or whitespace) to split key and value
   - Unescape both key and value using custom unescape logic
3. Return LinkedHashMap to preserve property order

**Unescape Logic** (private helper method):
- Process escape sequences in order:
  - `\\` → `\` (literal backslash)
  - `\=` → `=` (literal equals)
  - `\:` → `:` (literal colon)
  - `\t` → tab character
  - `\n` → newline
  - `\r` → carriage return
  - `\f` → form feed
  - `\ ` → space (escaped space)
  - `\#` → `#` (literal hash)
  - `\!` → `!` (literal exclamation)
  - `\uXXXX` → Unicode character (4 hex digits)
- Handle unknown escape sequences by keeping backslash and character

**Separator Detection** (private helper method):
- Scan string character by character
- Track backslash escaping (count consecutive backslashes)
- First unescaped `=`, `:`, or whitespace is the separator
- Return index of separator or -1 if not found

**Logging**:
- DEBUG: Log start of parsing, number of properties parsed
- DEBUG: Log each property key-value pair (truncate long values)
- WARN: Log malformed lines that couldn't be parsed
- INFO: Log completion with total count

**Error Handling**:
- Throw IOException for stream reading errors
- Log and skip malformed lines (don't throw)
- Close BufferedReader in try-with-resources

**Notes**:
- Do NOT close the input InputStream (caller owns it)
- Use LinkedHashMap to preserve insertion order for deterministic reports
- Import: java.io.*, java.nio.charset.StandardCharsets, java.util.LinkedHashMap, java.util.Map
- Reference the PDFBox documentation examples in `lib-doc/pdfbox/code-examples.md` for properties handling patterns

### src/test/java/com/example/pdfformfiller/PropertyParserTest.java(NEW)

Create comprehensive test class for PropertyParser using TDD approach:

**Purpose**: Verify PropertyParser correctly handles standard properties syntax and custom escape sequences.

**Test Organization** (use JUnit 5 @Nested classes):

**1. Basic Parsing Tests** (@Nested class "BasicParsing"):
- `testEmptyInput()` - empty stream returns empty map
- `testSingleProperty()` - "key=value" returns map with one entry
- `testMultipleProperties()` - multiple key=value pairs
- `testEqualsSignSeparator()` - "key=value" works
- `testColonSeparator()` - "key:value" works
- `testWhitespaceSeparator()` - "key value" works
- `testEmptyValue()` - "key=" returns empty string value
- `testWhitespaceAroundSeparator()` - "key = value" trims correctly

**2. Comment and Blank Line Tests** (@Nested class "CommentsAndBlanks"):
- `testHashComment()` - lines starting with # are ignored
- `testExclamationComment()` - lines starting with ! are ignored
- `testBlankLines()` - empty lines are ignored
- `testCommentAfterProperty()` - comments on same line as property (if supported)

**3. Custom Escape Sequence Tests** (@Nested class "CustomEscapes"):
- `testEscapedEquals()` - "key\\=name=value" → key is "key=name"
- `testEscapedBackslash()` - "path=C:\\\\temp" → value is "C:\\temp"
- `testEscapedEqualsInKey()` - "field\\=value=data" → key is "field=value"
- `testMultipleEscapedBackslashes()` - "path\\\\to\\\\file=value" → key is "path\\to\\file"
- `testMixedEscapes()` - combination of \\= and \\\\ in same property

**4. Standard Escape Sequence Tests** (@Nested class "StandardEscapes"):
- `testTabEscape()` - "\\t" → tab character
- `testNewlineEscape()` - "\\n" → newline
- `testCarriageReturnEscape()` - "\\r" → carriage return
- `testSpaceEscape()` - "\\ " → space
- `testUnicodeEscape()` - "\\u00E9" → "é"
- `testMultipleUnicodeEscapes()` - "\\u4F60\\u597D" → "你好"

**5. Line Continuation Tests** (@Nested class "LineContinuation"):
- `testSimpleContinuation()` - line ending with \\ continues to next line
- `testMultipleLineContinuation()` - three lines joined
- `testEvenBackslashesNoContinuation()` - \\\\ at end doesn't continue

**6. Edge Cases** (@Nested class "EdgeCases"):
- `testKeyWithDot()` - "user.name=value" (for special-chars PDF)
- `testKeyWithUnderscore()` - "user_email=value"
- `testKeyWithHyphen()` - "address-line1=value"
- `testLeadingWhitespace()` - "  key=value" trims leading space
- `testTrailingWhitespace()` - "key=value  " behavior
- `testVeryLongValue()` - value with 1000+ characters
- `testSpecialCharactersInValue()` - value with quotes, brackets, etc.

**7. Real-World Scenarios** (@Nested class "RealWorldScenarios"):
- `testSimpleFormProperties()` - properties matching sample-form-simple.pdf fields
- `testComplexFormProperties()` - properties matching sample-form-complex.pdf fields
- `testSpecialCharsFormProperties()` - properties matching sample-form-special-chars.pdf fields with proper escaping

**Test Utilities**:
- Helper method: `Map<String, String> parseString(String content)` - creates ByteArrayInputStream from string and calls PropertyParser.parse()
- Helper method: `void assertProperty(Map<String, String> map, String key, String expectedValue)` - asserts key exists with expected value

**Test Data Examples**:
- For special-chars tests: "field\\=value=some data" should parse to key="field=value", value="some data"
- For backslash tests: "path\\\\to\\\\file=C:\\\\temp" should parse to key="path\\to\\file", value="C:\\temp"

**Assertions** (use AssertJ):
- assertThat(map).isEmpty()
- assertThat(map).hasSize(n)
- assertThat(map).containsEntry(key, value)
- assertThat(map).containsKeys(key1, key2, ...)
- assertThat(value).isEqualTo(expected)

**Notes**:
- Use ByteArrayInputStream with UTF-8 encoding for all tests
- Test both successful parsing and edge cases
- No mocks - test the actual parsing logic
- Import: org.junit.jupiter.api.*, org.assertj.core.api.Assertions.*, java.io.ByteArrayInputStream, java.nio.charset.StandardCharsets, java.util.Map

### src/main/java/com/example/pdfformfiller/PdfFormProcessor.java(NEW)

References: 

- lib-doc/pdfbox/pdfbox-forms-documentation.md
- lib-doc/pdfbox/code-examples.md
- src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldResult.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldStatus.java(NEW)

Create class for filling PDF forms from property maps:

**Purpose**: Fill PDF form fields from a property map and generate processing report data.

**Class Structure** (use Lombok @Slf4j for logging):
- Public method: `ReportData fillForm(InputStream pdfInput, Map<String, String> properties, OutputStream pdfOutput) throws IOException`
- Private helper methods for field type handling

**Main Algorithm** (fillForm method):
1. Load PDF document from InputStream using `Loader.loadPDF(pdfInput)`
2. Get AcroForm: `document.getDocumentCatalog().getAcroForm()`
3. If AcroForm is null, log error and throw IOException
4. Initialize result tracking: List<FieldResult>, Set<String> of filled field names
5. For each property entry:
   - Get PDF field by name: `acroForm.getField(propertyKey)`
   - If field is null: create FieldResult with FIELD_NOT_FOUND status
   - If field exists: call helper method to fill based on field type
   - Track result in FieldResult list
6. Identify unfilled PDF fields: iterate all acroForm.getFields(), check if not in filled set
7. Call `acroForm.refreshAppearances()` to update visual appearance
8. Save document to OutputStream: `document.save(pdfOutput)`
9. Close document (use try-with-resources)
10. Build and return ReportData with results

**Field Type Handling** (private helper methods):

**fillTextField(PDField field, String value)**:
- Cast to PDTextField (check instanceof first)
- Call `field.setValue(value)`
- Log DEBUG: field name and value (truncate long values)
- Return FieldResult with FILLED status
- Catch IOException, log ERROR, return FieldResult with ERROR status

**fillCheckBox(PDField field, String value)**:
- Cast to PDCheckBox
- Parse value: "true", "yes", "1", "on" (case-insensitive) → check()
- Otherwise → unCheck()
- Log DEBUG: field name and checked state
- Return FieldResult with FILLED status
- Catch IOException, log ERROR, return FieldResult with ERROR status

**fillRadioButton(PDField field, String value)**:
- Cast to PDRadioButton
- Get available options: `radioButton.getOnValues()`
- If value matches one of the options (case-sensitive): call `field.setValue(value)`
- Otherwise: log WARN about invalid option, return FieldResult with ERROR status
- Log DEBUG: field name and selected value
- Return FieldResult with FILLED status
- Catch IOException, log ERROR, return FieldResult with ERROR status

**fillChoiceField(PDField field, String value)**:
- Cast to PDChoice (handles both PDComboBox and PDListBox)
- Get available options: `choiceField.getOptions()`
- If value is in options: call `field.setValue(value)`
- Otherwise: log WARN about invalid option, try to set anyway (some PDFs allow free text)
- Log DEBUG: field name and selected value
- Return FieldResult with FILLED status
- Catch IOException, log ERROR, return FieldResult with ERROR status

**fillGenericField(PDField field, String value)**:
- Fallback for unknown field types
- Try `field.setValue(value)`
- Log DEBUG: field name, type, and value
- Return FieldResult with FILLED status
- Catch IOException, log ERROR, return FieldResult with ERROR status

**Field Type Dispatch** (private helper method):
- Check field type using instanceof:
  - PDTextField → fillTextField
  - PDCheckBox → fillCheckBox
  - PDRadioButton → fillRadioButton
  - PDChoice → fillChoiceField
  - Otherwise → fillGenericField

**Logging**:
- INFO: Log start of processing with property count
- DEBUG: Log each field fill attempt with field name and value
- WARN: Log when property key doesn't match any PDF field
- WARN: Log when field value is invalid for field type
- ERROR: Log IOException during field filling
- INFO: Log completion with statistics (filled, not found, errors)

**Error Handling**:
- Throw IOException if PDF cannot be loaded or saved
- Throw IOException if AcroForm is null (not a fillable form)
- Catch and log IOException for individual field operations, continue processing
- Use try-with-resources for PDDocument
- Do NOT close input/output streams (caller owns them)

**Notes**:
- Import PDFBox classes: org.apache.pdfbox.Loader, org.apache.pdfbox.pdmodel.PDDocument, org.apache.pdfbox.pdmodel.interactive.form.*
- Import model classes from `com.example.pdfformfiller.model` package
- Import: java.io.*, java.time.Instant, java.util.*, java.util.stream.Collectors
- Reference PDFBox documentation in `lib-doc/pdfbox/pdfbox-forms-documentation.md` and `lib-doc/pdfbox/code-examples.md` for field handling patterns

### src/test/java/com/example/pdfformfiller/PdfFormProcessorTest.java(NEW)

References: 

- src/test/resources/pdfs/README.md
- src/main/java/com/example/pdfformfiller/PdfFormProcessor.java(NEW)
- src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)

Create comprehensive test class for PdfFormProcessor using TDD approach with real PDFs:

**Purpose**: Verify PdfFormProcessor correctly fills PDF forms and generates accurate report data.

**Test Setup** (class-level fields and @BeforeEach):
- Helper method: `byte[] loadPdfResource(String filename)` - loads PDF from src/test/resources/pdfs/ into byte array
- Helper method: `Map<String, String> verifyFilledPdf(byte[] pdfBytes)` - loads PDF from byte array, reads all field values, returns map of field name → value
- Helper method: `String getFieldValue(PDField field)` - gets value as string, handles different field types (checkbox returns "checked"/"unchecked", radio returns selected option)
- Helper method: `boolean isCheckboxChecked(PDCheckBox checkbox)` - uses checkbox.isChecked()

**Test Organization** (use JUnit 5 @Nested classes):

**1. Simple Form Tests** (@Nested class "SimpleFormTests"):
- `testFillAllTextFields()` - fill all 5 text fields in sample-form-simple.pdf, verify all filled correctly
- `testFillSubsetOfFields()` - fill only 3 of 5 fields, verify 3 filled and 2 unfilled
- `testEmptyPropertyMap()` - pass empty map, verify no fields filled, all fields in unfilled list
- `testFieldNotFound()` - property key "nonexistent" not in PDF, verify FieldResult with FIELD_NOT_FOUND status
- `testMultilineTextField()` - fill "address" field with multiline text (with \n), verify preserved

**2. Complex Form Tests** (@Nested class "ComplexFormTests"):
- `testFillTextFields()` - fill name, email, country, comments fields
- `testFillCheckboxes()` - fill subscribe=true, agreeToTerms=false, newsletter=yes, verify checked states
- `testCheckboxVariations()` - test various true values: "true", "yes", "1", "on" (case-insensitive)
- `testRadioButtonSelection()` - fill gender=Male, verify Male selected and Female/Other not selected
- `testRadioButtonEmploymentType()` - fill employmentType=PartTime, verify correct selection
- `testInvalidRadioValue()` - fill gender=Invalid, verify ERROR status in FieldResult
- `testDropdownSelection()` - fill department=Engineering, verify selected
- `testInvalidDropdownValue()` - fill department=InvalidDept, verify behavior (may succeed with warning)
- `testMixedFieldTypes()` - fill all 10 fields with various types, verify all correct

**3. Special Characters Form Tests** (@Nested class "SpecialCharsFormTests"):
- `testFieldWithDot()` - fill "user.name" field
- `testFieldWithUnderscore()` - fill "user_email" field
- `testFieldWithHyphen()` - fill "address-line1" field
- `testFieldWithEquals()` - fill "field=value" field (property key must match exactly)
- `testFieldWithBackslashes()` - fill "path\\to\\file" field
- `testAllSpecialCharFields()` - fill all 5 special-char fields, verify all filled

**4. Report Data Tests** (@Nested class "ReportDataTests"):
- `testReportDataAllFilled()` - all properties match fields, verify counts: filled=N, notFound=0, error=0
- `testReportDataSomeNotFound()` - some properties don't match, verify notFound count
- `testReportDataUnfilledFields()` - PDF has more fields than properties, verify unfilledPdfFields list
- `testReportDataWithErrors()` - invalid field values cause errors, verify error count
- `testReportDataTimestamp()` - verify processingTimestamp is recent (within last minute)
- `testReportDataFieldResults()` - verify FieldResult objects have correct propertyKey, propertyValue, pdfFieldName, status, fieldType

**5. Edge Cases** (@Nested class "EdgeCases"):
- `testEmptyFieldValue()` - property with empty value "", verify field set to empty string
- `testVeryLongFieldValue()` - value with 1000+ characters, verify truncation or full storage
- `testSpecialCharactersInValue()` - value with quotes, brackets, newlines, verify preserved
- `testUnicodeInValue()` - value with Unicode characters (emoji, Chinese), verify preserved
- `testNullPropertyValue()` - should not happen with PropertyParser, but test defensive handling
- `testReadOnlyField()` - if PDF has read-only field, verify behavior (may skip or error)

**6. Error Handling Tests** (@Nested class "ErrorHandling"):
- `testInvalidPdfInput()` - pass non-PDF bytes, verify IOException thrown
- `testEmptyPdfInput()` - pass empty byte array, verify IOException thrown
- `testPdfWithoutAcroForm()` - PDF without form fields, verify IOException thrown with clear message
- `testCorruptedPdf()` - truncated PDF bytes, verify IOException thrown

**7. Stream Handling Tests** (@Nested class "StreamHandling"):
- `testInputStreamNotClosed()` - verify input stream remains open after fillForm (caller owns it)
- `testOutputStreamNotClosed()` - verify output stream remains open after fillForm
- `testLargePdfProcessing()` - test with larger PDF (if available), verify memory efficiency

**Test Data Creation**:
- Use Map.of() or LinkedHashMap for property maps
- Example for simple form: Map.of("firstName", "Max", "lastName", "Mustermann", "email", "max@example.com", "phone", "+49123456789", "address", "Musterstraße 123\n12345 Musterstadt")
- Example for checkboxes: Map.of("subscribe", "true", "agreeToTerms", "false", "newsletter", "yes")
- Example for radio: Map.of("gender", "Male", "employmentType", "FullTime")

**Verification Strategy**:
1. Call PdfFormProcessor.fillForm() with test PDF and properties
2. Capture output in ByteArrayOutputStream
3. Load filled PDF from output bytes using Loader.loadPDF(new ByteArrayInputStream(bytes))
4. Get AcroForm and read field values using acroForm.getField(name).getValueAsString()
5. For checkboxes: use PDCheckBox.isChecked()
6. For radio buttons: verify getValue() matches expected option
7. Assert field values match expected values
8. Verify ReportData statistics match expectations

**Assertions** (use AssertJ):
- assertThat(reportData.getTotalProperties()).isEqualTo(n)
- assertThat(reportData.getFilledCount()).isEqualTo(n)
- assertThat(reportData.getNotFoundCount()).isEqualTo(n)
- assertThat(reportData.getErrorCount()).isEqualTo(n)
- assertThat(reportData.getUnfilledPdfFields()).containsExactlyInAnyOrder(field1, field2)
- assertThat(fieldValue).isEqualTo(expectedValue)
- assertThat(checkbox.isChecked()).isTrue()
- assertThatThrownBy(() -> ...).isInstanceOf(IOException.class).hasMessageContaining("...")

**Notes**:
- Use real PDF files from src/test/resources/pdfs/
- Use ByteArrayInputStream and ByteArrayOutputStream for in-memory testing
- No mocks - test with real PDFBox operations
- Each test should be independent (load fresh PDF each time)
- Import: org.junit.jupiter.api.*, org.assertj.core.api.Assertions.*, org.apache.pdfbox.*, java.io.*, java.util.*, java.time.Instant
- Reference test PDF documentation in `src/test/resources/pdfs/README.md` for field names and types

### src/main/java/com/example/pdfformfiller/ReportGenerator.java(NEW)

References: 

- src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldResult.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldStatus.java(NEW)

Create class for generating text reports from processing results:

**Purpose**: Generate human-readable text report from ReportData.

**Class Structure** (use Lombok @Slf4j for logging):
- Public method: `void generate(ReportData reportData, OutputStream output) throws IOException`
- Private helper methods for formatting sections

**Report Format** (plain text with sections):

**Header Section**:
```
========================================
PDF Form Processing Report
========================================
Processing Time: [ISO-8601 timestamp]

```

**Summary Section**:
```
SUMMARY
-------
Total Properties: [N]
Successfully Filled: [N]
Fields Not Found: [N]
Errors: [N]
Unfilled PDF Fields: [N]

```

**Filled Fields Section** (if any):
```
FILLED FIELDS
-------------
[propertyKey] -> [pdfFieldName] ([fieldType])
  Value: [propertyValue]
[repeat for each filled field]

```

**Fields Not Found Section** (if any):
```
FIELDS NOT FOUND IN PDF
-----------------------
[propertyKey]
  Value: [propertyValue]
[repeat for each not found]

```

**Errors Section** (if any):
```
ERRORS
------
[propertyKey] -> [pdfFieldName]
  Value: [propertyValue]
  Error: [errorMessage]
[repeat for each error]

```

**Unfilled PDF Fields Section** (if any):
```
UNFILLED PDF FIELDS
-------------------
[fieldName]
[repeat for each unfilled field]

```

**Footer Section**:
```
========================================
End of Report
========================================
```

**Implementation Details**:
1. Create BufferedWriter wrapping OutputStream with UTF-8 encoding
2. Write header with timestamp formatted as ISO-8601 (use DateTimeFormatter.ISO_INSTANT)
3. Write summary section with counts from ReportData
4. Iterate fieldResults, group by status, write appropriate sections
5. Write unfilled PDF fields section
6. Write footer
7. Flush writer (do NOT close - caller owns stream)

**Helper Methods**:
- `String formatTimestamp(Instant timestamp)` - format as ISO-8601
- `String truncateValue(String value, int maxLength)` - truncate long values with "..." suffix
- `void writeSection(BufferedWriter writer, String title, List<String> lines)` - write section with title and lines

**Value Truncation**:
- Truncate property values longer than 100 characters for readability
- Show first 97 characters + "..."
- Log DEBUG when truncating

**Logging**:
- DEBUG: Log start of report generation
- DEBUG: Log each section being written
- INFO: Log completion of report generation
- ERROR: Log IOException during writing

**Error Handling**:
- Throw IOException for stream writing errors
- Use try-with-resources for BufferedWriter (but don't close underlying OutputStream)
- Flush writer before returning to ensure all data written

**Notes**:
- Use UTF-8 encoding: new OutputStreamWriter(output, StandardCharsets.UTF_8)
- Do NOT close the output stream (caller owns it)
- Use platform-independent line separator: System.lineSeparator() or "\n"
- Import: java.io.*, java.nio.charset.StandardCharsets, java.time.format.DateTimeFormatter, java.util.List, java.util.stream.Collectors
- Import model classes from `com.example.pdfformfiller.model` package

### src/test/java/com/example/pdfformfiller/ReportGeneratorTest.java(NEW)

References: 

- src/main/java/com/example/pdfformfiller/ReportGenerator.java(NEW)
- src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldResult.java(NEW)

Create comprehensive test class for ReportGenerator using TDD approach:

**Purpose**: Verify ReportGenerator produces correct text reports from ReportData.

**Test Setup** (helper methods):
- Helper method: `String generateReport(ReportData reportData)` - calls ReportGenerator.generate() with ByteArrayOutputStream, returns string
- Helper method: `ReportData.ReportDataBuilder createBaseReportData()` - returns builder with common fields set (timestamp, empty lists)
- Helper method: `FieldResult createFilledResult(String key, String value, String fieldName, String fieldType)` - creates FieldResult with FILLED status
- Helper method: `FieldResult createNotFoundResult(String key, String value)` - creates FieldResult with FIELD_NOT_FOUND status
- Helper method: `FieldResult createErrorResult(String key, String value, String fieldName, String error)` - creates FieldResult with ERROR status

**Test Organization** (use JUnit 5 @Nested classes):

**1. Report Structure Tests** (@Nested class "ReportStructure"):
- `testReportHasHeader()` - verify report starts with header section and title
- `testReportHasFooter()` - verify report ends with footer section
- `testReportHasSummarySection()` - verify summary section present with all counts
- `testReportHasTimestamp()` - verify timestamp in ISO-8601 format
- `testReportSectionsInOrder()` - verify sections appear in correct order: header, summary, filled, not found, errors, unfilled, footer

**2. Summary Section Tests** (@Nested class "SummarySection"):
- `testSummaryAllFilled()` - all properties filled, verify counts: total=5, filled=5, notFound=0, errors=0, unfilled=0
- `testSummarySomeNotFound()` - some not found, verify counts: total=5, filled=3, notFound=2, errors=0, unfilled=2
- `testSummaryWithErrors()` - some errors, verify counts: total=5, filled=2, notFound=1, errors=2, unfilled=0
- `testSummaryEmpty()` - no properties, verify counts all zero

**3. Filled Fields Section Tests** (@Nested class "FilledFieldsSection"):
- `testFilledFieldsPresent()` - verify section title "FILLED FIELDS" present when fields filled
- `testFilledFieldsAbsent()` - verify section absent when no fields filled
- `testFilledFieldFormat()` - verify format: "key -> fieldName (fieldType)\n  Value: value"
- `testMultipleFilledFields()` - verify all filled fields listed
- `testFilledFieldValueTruncation()` - value > 100 chars truncated with "..."

**4. Not Found Section Tests** (@Nested class "NotFoundSection"):
- `testNotFoundSectionPresent()` - verify section title "FIELDS NOT FOUND IN PDF" present when fields not found
- `testNotFoundSectionAbsent()` - verify section absent when all fields found
- `testNotFoundFieldFormat()` - verify format: "key\n  Value: value"
- `testMultipleNotFoundFields()` - verify all not-found fields listed

**5. Errors Section Tests** (@Nested class "ErrorsSection"):
- `testErrorsSectionPresent()` - verify section title "ERRORS" present when errors occurred
- `testErrorsSectionAbsent()` - verify section absent when no errors
- `testErrorFieldFormat()` - verify format: "key -> fieldName\n  Value: value\n  Error: errorMessage"
- `testMultipleErrors()` - verify all errors listed with messages

**6. Unfilled PDF Fields Section Tests** (@Nested class "UnfilledFieldsSection"):
- `testUnfilledSectionPresent()` - verify section title "UNFILLED PDF FIELDS" present when PDF has unfilled fields
- `testUnfilledSectionAbsent()` - verify section absent when all PDF fields filled
- `testUnfilledFieldFormat()` - verify format: one field name per line
- `testMultipleUnfilledFields()` - verify all unfilled fields listed

**7. Real-World Scenarios** (@Nested class "RealWorldScenarios"):
- `testSimpleFormAllFilled()` - simulate sample-form-simple.pdf with all 5 fields filled
- `testSimpleFormPartiallyFilled()` - 3 of 5 fields filled, 2 unfilled
- `testComplexFormMixedResults()` - sample-form-complex.pdf with filled, not found, and errors
- `testSpecialCharsFormReport()` - sample-form-special-chars.pdf with special character field names
- `testEmptyPropertiesReport()` - no properties provided, all PDF fields unfilled

**8. Edge Cases** (@Nested class "EdgeCases"):
- `testEmptyReportData()` - ReportData with all empty lists and zero counts
- `testVeryLongFieldName()` - field name with 200+ characters
- `testVeryLongValue()` - value with 1000+ characters, verify truncation
- `testSpecialCharactersInValue()` - value with newlines, tabs, quotes, verify escaped or preserved
- `testUnicodeInReport()` - field names and values with Unicode characters
- `testNullErrorMessage()` - FieldResult with ERROR status but null errorMessage

**9. Stream Handling Tests** (@Nested class "StreamHandling"):
- `testOutputStreamNotClosed()` - verify output stream remains open after generate()
- `testOutputStreamFlushed()` - verify data written to stream (not buffered)
- `testMultipleReportsToSameStream()` - generate two reports to same stream, verify both present

**Test Data Creation**:
- Use ReportData.builder() to create test data
- Use FieldResult.builder() to create field results
- Example: ReportData.builder().totalProperties(5).fieldResults(List.of(...)).unfilledPdfFields(List.of(...)).processingTimestamp(Instant.now()).build()

**Assertions** (use AssertJ):
- assertThat(report).contains("PDF Form Processing Report")
- assertThat(report).contains("Total Properties: 5")
- assertThat(report).contains("Successfully Filled: 3")
- assertThat(report).containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}") // ISO-8601 timestamp
- assertThat(report).contains("firstName -> firstName (PDTextField)")
- assertThat(report).doesNotContain("ERRORS") // when no errors
- assertThat(report.lines().count()).isGreaterThan(10) // has multiple lines

**Notes**:
- Use ByteArrayOutputStream for in-memory testing
- Convert to string with UTF-8: baos.toString(StandardCharsets.UTF_8)
- Test both presence and absence of sections based on data
- Verify exact format of each section
- Test with realistic ReportData from PdfFormProcessor scenarios
- Import: org.junit.jupiter.api.*, org.assertj.core.api.Assertions.*, java.io.*, java.nio.charset.StandardCharsets, java.time.Instant, java.util.List
- Reference model classes from `com.example.pdfformfiller.model` package

### src/test/resources/properties/simple-form-all-fields.properties(NEW)

Create test properties file for sample-form-simple.pdf with all fields filled:

**Content**:
```properties
# Test properties for sample-form-simple.pdf
# All fields filled with sample data

firstName=Max
lastName=Mustermann
email=max.mustermann@example.com
phone=+49 123 456789
address=Musterstraße 123\n12345 Musterstadt\nDeutschland
```

**Purpose**: Test data for PropertyParser and PdfFormProcessor integration tests.

### src/test/resources/properties/complex-form-mixed.properties(NEW)

Create test properties file for sample-form-complex.pdf with mixed field types:

**Content**:
```properties
# Test properties for sample-form-complex.pdf
# Mixed field types: text, checkboxes, radio buttons, dropdown

name=Max Mustermann
email=max@example.com
subscribe=true
agreeToTerms=yes
newsletter=false
gender=Male
employmentType=FullTime
department=Engineering
country=Germany
comments=This is a test comment.\nIt spans multiple lines.\nThird line here.
```

**Purpose**: Test data for complex form with various field types.

### src/test/resources/properties/special-chars-escaped.properties(NEW)

Create test properties file for sample-form-special-chars.pdf with properly escaped field names:

**Content**:
```properties
# Test properties for sample-form-special-chars.pdf
# Field names contain special characters that require escaping

user.name=John Doe
user_email=john.doe@example.com
address-line1=123 Main Street
field\=value=This field name contains an equals sign
path\\to\\file=C:\\Users\\Documents\\file.txt
```

**Purpose**: Test data for fields with special characters in names, demonstrating escape sequence handling.

**Notes**:
- `field\=value` - the field name is "field=value" (equals sign escaped)
- `path\\to\\file` - the field name is "path\to\file" (backslashes escaped)

### src/test/resources/properties/README.md(NEW)

Create documentation for test properties files:

**Content**:
```markdown
# Test Properties Files

This directory contains sample properties files for testing the PDF form filler.

## Files

### simple-form-all-fields.properties
- **Purpose**: Test data for `sample-form-simple.pdf`
- **Fields**: All 5 text fields filled
- **Use Case**: Happy path testing with simple text fields

### complex-form-mixed.properties
- **Purpose**: Test data for `sample-form-complex.pdf`
- **Fields**: All 10 fields with mixed types (text, checkbox, radio, dropdown)
- **Use Case**: Testing various field types and values

### special-chars-escaped.properties
- **Purpose**: Test data for `sample-form-special-chars.pdf`
- **Fields**: Field names with special characters (dots, underscores, hyphens, equals, backslashes)
- **Use Case**: Testing escape sequence handling in property keys
- **Note**: Demonstrates proper escaping: `field\=value` for field name "field=value", `path\\to\\file` for "path\to\file"

## Escape Sequences

The property parser supports these escape sequences:
- `\\` → `\` (literal backslash)
- `\=` → `=` (literal equals sign)
- `\:` → `:` (literal colon)
- `\t` → tab character
- `\n` → newline
- `\r` → carriage return
- `\ ` → space (escaped space)
- `\uXXXX` → Unicode character (4 hex digits)

## Usage in Tests

These files can be loaded in tests using:
```java
InputStream input = getClass().getResourceAsStream("/properties/simple-form-all-fields.properties");
Map<String, String> properties = PropertyParser.parse(input);
```
```

**Purpose**: Document the test properties files and their usage.

### src/test/java/com/example/pdfformfiller/TestUtils.java(NEW)

References: 

- src/main/java/com/example/pdfformfiller/PropertyParser.java(NEW)
- src/main/java/com/example/pdfformfiller/model/ReportData.java(NEW)
- src/main/java/com/example/pdfformfiller/model/FieldResult.java(NEW)

Create utility class with helper methods for tests:

**Purpose**: Provide common test utilities to reduce code duplication across test classes.

**Class Structure** (utility class with static methods):

**Resource Loading**:
- `static byte[] loadPdfResource(String filename)` - loads PDF from src/test/resources/pdfs/, returns byte array
- `static InputStream loadPropertiesResource(String filename)` - loads properties file from src/test/resources/properties/
- `static String loadTextResource(String filename)` - loads any text file as string with UTF-8 encoding

**PDF Verification**:
- `static Map<String, String> readPdfFieldValues(byte[] pdfBytes)` - loads PDF, reads all field values, returns map of field name → value
- `static String getFieldValue(PDField field)` - gets field value as string, handles different field types:
  - PDTextField: getValueAsString()
  - PDCheckBox: "checked" if isChecked(), "unchecked" otherwise
  - PDRadioButton: getValue() (selected option name)
  - PDChoice: getValue()
  - Other: getValueAsString()
- `static boolean isCheckboxChecked(PDCheckBox checkbox)` - returns checkbox.isChecked()
- `static List<String> getAllPdfFieldNames(byte[] pdfBytes)` - loads PDF, returns list of all field names

**Property Parsing**:
- `static Map<String, String> parsePropertiesString(String content)` - creates ByteArrayInputStream from string, calls PropertyParser.parse()
- `static Map<String, String> parsePropertiesResource(String filename)` - loads and parses properties file from resources

**Stream Utilities**:
- `static ByteArrayInputStream toInputStream(String content)` - converts string to ByteArrayInputStream with UTF-8
- `static ByteArrayInputStream toInputStream(byte[] bytes)` - wraps byte array in ByteArrayInputStream
- `static String fromOutputStream(ByteArrayOutputStream baos)` - converts ByteArrayOutputStream to string with UTF-8

**Assertion Helpers**:
- `static void assertPropertyEquals(Map<String, String> map, String key, String expectedValue)` - asserts key exists with expected value
- `static void assertPdfFieldValue(byte[] pdfBytes, String fieldName, String expectedValue)` - loads PDF and asserts field value
- `static void assertCheckboxState(byte[] pdfBytes, String fieldName, boolean expectedChecked)` - loads PDF and asserts checkbox state

**Test Data Builders**:
- `static ReportData.ReportDataBuilder createBaseReportData()` - returns builder with timestamp and empty lists
- `static FieldResult createFilledResult(String key, String value, String fieldName, String fieldType)` - creates FieldResult with FILLED status
- `static FieldResult createNotFoundResult(String key, String value)` - creates FieldResult with FIELD_NOT_FOUND status
- `static FieldResult createErrorResult(String key, String value, String fieldName, String error)` - creates FieldResult with ERROR status

**Implementation Notes**:
- Use try-with-resources for all resource handling
- Throw RuntimeException wrapping checked exceptions (this is test code)
- Use StandardCharsets.UTF_8 for all encoding
- Close PDDocument after reading fields
- Log errors with descriptive messages

**Error Handling**:
- Wrap IOException in RuntimeException with descriptive message
- Include filename/resource name in error messages
- Use assertThat().isNotNull() for resource loading verification

**Notes**:
- This is a utility class, make constructor private
- All methods are static
- Import: java.io.*, java.nio.charset.StandardCharsets, java.util.*, org.apache.pdfbox.*, org.assertj.core.api.Assertions, java.time.Instant
- Import model classes from `com.example.pdfformfiller.model` package
- Reference test resources in `src/test/resources/pdfs/` and `src/test/resources/properties/`