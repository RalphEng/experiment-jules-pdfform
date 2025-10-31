# Test PDF Forms

This directory contains fillable PDF forms for testing purposes. These PDFs contain AcroForm fields with the exact field names specified below.

## sample-form-simple.pdf
- **Purpose:** Basic text field testing
- **Fields:**
  - `firstName` (text)
  - `lastName` (text)
  - `email` (text)
  - `phone` (text)
  - `address` (text, multiline)

## sample-form-complex.pdf
- **Purpose:** Multiple field types including checkboxes, radio groups, and text fields
- **Fields:**
  - `name` (text)
  - `email` (text)
  - `subscribe` (checkbox, export values: "Yes"/"Off")
  - `agreeToTerms` (checkbox, export values: "Yes"/"Off")
  - `newsletter` (checkbox, export values: "Yes"/"Off")
  - `gender` (radio group, options: Male/Female/Other)
  - `employmentType` (radio group, options: FullTime/PartTime/Contractor)
  - `department` (text field - dropdown options: HR/Engineering/Marketing/Sales/Finance)
  - `country` (text field)
  - `comments` (text, multiline)

## sample-form-special-chars.pdf
- **Purpose:** Edge case testing for special characters in field names
- **Fields:**
  - `user.name` (text) - dot character in field name
  - `user_email` (text) - underscore character in field name
  - `address-line1` (text) - hyphen character in field name
  - `field=value` (text) - requires escaping in properties: `field\=value=...`
  - `path\\to\\file` (text) - requires escaping: `path\\\\to\\\\file=...`

All PDFs have been validated using Apache PDFBox to ensure field presence and correct field types.
