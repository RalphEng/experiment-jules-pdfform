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
