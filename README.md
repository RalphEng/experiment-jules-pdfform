# PDF Form Filler CLI

## Project Description
A Java CLI application for filling PDF forms from property files.

**Key Features:**
- Stream-based architecture
- Detailed logging
- Processing reports

**Technology Stack:**
- Java 17
- Maven
- PDFBox
- Picocli
- SLF4J/Logback
- Lombok

## Requirements
- Java 17 or higher
- Maven 3.6+ for building

## Building the Project
- `mvn clean install` - compile and run tests
- `mvn package` - create executable JAR
- Location of built JAR: `target/pdf-form-filler-1.0.0-SNAPSHOT.jar`

## Usage (placeholder for future implementation)
- Command syntax: `java -jar pdf-form-filler.jar --input <template.pdf> --properties <data.properties> --output <filled.pdf> [--log <logfile.log>] [--report <report.txt>]`
- Flag descriptions
- Example invocation

## Property File Format
- Standard Java properties format
- Escape sequences: `\=` for literal equals, `\\` for literal backslash
- Examples:
  - `firstName=Max`
  - `field\=name=value` (field name contains `=`)
  - `path\\to\\file=C:\\temp` (backslashes in value)

## Development
- Project structure overview
- Running tests: `mvn test`
- Test resources location: `src/test/resources/pdfs/`
- Logging configuration: `src/main/resources/logback.xml`

## Architecture (high-level)
- Stream-based core components for testability
- Separation of concerns: parsing, processing, reporting, CLI
- TDD approach with comprehensive unit tests

## License
Reference to LICENSE file
