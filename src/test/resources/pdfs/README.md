# Test PDF Forms

This directory contains placeholder PDF files for testing purposes. These files are currently empty and need to be replaced with actual PDF forms containing the specified fields.

## sample-form-simple.pdf
- **Purpose:** Basic text field testing
- **Fields:**
  - `firstName` (text)
  - `lastName` (text)
  - `email` (text)
  - `phone` (text)
  - `address` (text, multiline)

## sample-form-complex.pdf
- **Purpose:** Multiple field types
- **Fields:**
  - `name` (text)
  - `email` (text)
  - `subscribe` (checkbox, values: "Yes"/"Off")
  - `gender` (radio group, options: Male/Female/Other)
  - `department` (dropdown, options: HR/Engineering/Marketing/Sales/Finance)
  - `comments` (text, multiline)

## sample-form-special-chars.pdf
- **Purpose:** Edge case testing for special characters in field names
- **Fields:**
  - `user.name` (text)
  - `field=value` (text) - requires escaping in properties: `field\=value=...`
  - `path\\to\\file` (text) - requires escaping: `path\\\\to\\\\file=...`
