I have created the following plan after thorough exploration and analysis of the codebase. Follow the below plan verbatim. Trust the files and references. Do not re-verify what's written in the plan. Explore only when absolutely necessary. First implement all the proposed file changes and then I'll review all the changes together at the end.

### Observations

The core components are production-ready with comprehensive logging using Lombok's @Slf4j. The stream-based architecture enables clean separation between business logic and I/O operations. Logback is configured to write to both console (INFO level) and file (DEBUG level) with the file path configurable via `log.file` system property (default: pdf-form-filler.log). The task requires wrapping these components with file I/O handling and CLI argument parsing, with proper validation and error handling.

### Approach

Create a CLI layer with two components: (1) **FileIOService** - an adapter that converts file paths to streams and invokes core components, and (2) **PdfFormFillerCli** - a Picocli command class that handles argument parsing, validation, orchestration, and error handling. Configure log file path via system property before logging initialization. Use exit codes for different error scenarios (invalid arguments, file not found, processing errors).

### Reasoning

I explored the complete project structure and found all core components fully implemented with stream-based APIs: PropertyParser.parse(InputStream), PdfFormProcessor.fillForm(InputStream, Map, OutputStream), and ReportGenerator.generate(ReportData, OutputStream). The logback.xml already supports dynamic log file configuration via system property `log.file`. The pom.xml has Picocli 4.7.7 configured with main class set to PdfFormFillerCli. All model classes (ReportData, FieldResult, FieldStatus) are complete. Test utilities exist for integration testing.

## Mermaid Diagram

sequenceDiagram
    participant User
    participant CLI as PdfFormFillerCli<br/>(Picocli Command)
    participant FIO as FileIOService<br/>(I/O Adapter)
    participant PP as PropertyParser
    participant PFP as PdfFormProcessor
    participant RG as ReportGenerator
    participant FS as File System

    User->>CLI: java -jar app.jar<br/>-i template.pdf -p data.properties<br/>-o filled.pdf -l app.log -r report.txt
    
    Note over CLI: Configure log file path<br/>System.setProperty("log.file", "app.log")
    
    CLI->>CLI: Validate arguments<br/>(required flags, file existence)
    
    alt Validation fails
        CLI->>User: Exit code 1 or 2<br/>(error message to stderr)
    end
    
    CLI->>FIO: processForm(inputPath, propertiesPath, outputPath)
    
    FIO->>FIO: Validate input files exist & readable
    FIO->>FS: Read properties file
    FS-->>FIO: InputStream
    FIO->>PP: parse(InputStream)
    PP-->>FIO: Map<String, String>
    
    FIO->>FS: Read PDF template
    FS-->>FIO: InputStream
    FIO->>FIO: Create ByteArrayOutputStream
    
    FIO->>PFP: fillForm(pdfInputStream, properties, outputStream)
    PFP->>PFP: Load PDF, get AcroForm
    loop For each property
        PFP->>PFP: Fill field by type<br/>(text/checkbox/radio/choice)
    end
    PFP->>PFP: Refresh appearances
    PFP->>PFP: Save to OutputStream
    PFP-->>FIO: ReportData
    
    FIO->>FS: Write filled PDF to outputPath
    FIO-->>CLI: ReportData
    
    CLI->>FIO: generateReport(reportData, reportPath)
    FIO->>FS: Create report file
    FS-->>FIO: OutputStream
    FIO->>RG: generate(reportData, OutputStream)
    RG->>RG: Write header, summary,<br/>filled fields, errors, etc.
    RG->>FS: Flush to file
    FIO-->>CLI: (void)
    
    CLI->>User: Exit code 0<br/>"Success!" message to stdout
    
    Note over FS: Three files created:<br/>1. filled.pdf<br/>2. report.txt<br/>3. app.log

## Proposed File Changes

### src/main/java/com/example/pdfformfiller/FileIOService.java(NEW)

References: 

- src/main/java/com/example/pdfformfiller/PropertyParser.java
- src/main/java/com/example/pdfformfiller/PdfFormProcessor.java
- src/main/java/com/example/pdfformfiller/ReportGenerator.java
- src/main/java/com/example/pdfformfiller/model/ReportData.java

Create a service class that acts as an adapter between file-based operations and stream-based core components.

**Purpose**: Encapsulate all file I/O operations, providing a clean interface for the CLI layer while delegating business logic to core components.

**Class Structure** (use Lombok @Slf4j for logging):

**Public Method - Main Orchestration**:
- `ReportData processForm(Path inputPdf, Path propertiesFile, Path outputPdf)` - orchestrates the complete form filling workflow
  - Validates input files exist and are readable
  - Reads properties file into InputStream, calls PropertyParser.parse()
  - Reads PDF template into InputStream
  - Creates ByteArrayOutputStream for PDF output
  - Calls PdfFormProcessor.fillForm() with streams
  - Writes filled PDF from ByteArrayOutputStream to outputPdf file
  - Returns ReportData for report generation
  - Throws IOException for file operations or processing errors

**Public Method - Report Generation**:
- `void generateReport(ReportData reportData, Path reportFile)` - generates text report to file
  - Creates parent directories if needed
  - Opens FileOutputStream for report file
  - Calls ReportGenerator.generate() with stream
  - Closes stream properly
  - Throws IOException for file operations

**Private Helper Methods**:
- `void validateInputFile(Path file, String description)` - validates file exists, is readable, is regular file
  - Throws IOException with descriptive message if validation fails
  - Log DEBUG: validation success
- `void ensureParentDirectoryExists(Path file)` - creates parent directories if needed
  - Uses Files.createDirectories()
  - Log DEBUG: directory creation
- `byte[] readFileToBytes(Path file)` - reads entire file into byte array
  - Uses Files.readAllBytes()
  - Log DEBUG: file size read
- `void writeBytesToFile(byte[] data, Path file)` - writes byte array to file
  - Uses Files.write() with StandardOpenOption.CREATE, TRUNCATE_EXISTING, WRITE
  - Log DEBUG: bytes written

**Workflow in processForm()**:
1. Log INFO: Starting form processing with file paths
2. Validate inputPdf exists and is readable
3. Validate propertiesFile exists and is readable
4. Ensure outputPdf parent directory exists
5. Read properties file into InputStream using Files.newInputStream()
6. Parse properties using PropertyParser.parse()
7. Log INFO: Parsed N properties
8. Read PDF template into InputStream using Files.newInputStream()
9. Create ByteArrayOutputStream for PDF output
10. Call PdfFormProcessor.fillForm() with PDF input stream, properties map, and output stream
11. Write ByteArrayOutputStream to outputPdf file
12. Log INFO: Successfully wrote filled PDF to output path
13. Return ReportData

**Error Handling**:
- Throw IOException with descriptive messages for all file operations
- Include file paths in error messages for debugging
- Use try-with-resources for all streams to ensure proper cleanup
- Log ERROR: exceptions with full context (file paths, operation)

**Logging Strategy**:
- INFO: Major workflow steps (starting processing, parsed properties, wrote output)
- DEBUG: Detailed operations (file validation, directory creation, byte counts)
- ERROR: All exceptions with context

**Imports**:
- java.io.* (IOException, InputStream, OutputStream, ByteArrayOutputStream)
- java.nio.file.* (Path, Files, StandardOpenOption)
- com.example.pdfformfiller.* (PropertyParser, PdfFormProcessor, ReportGenerator)
- com.example.pdfformfiller.model.ReportData
- lombok.extern.slf4j.Slf4j

**Design Rationale**:
- Separates file I/O concerns from business logic
- Provides testable interface (can mock Path operations in tests)
- Centralizes file validation logic
- Maintains stream-based architecture internally
- Clear error messages for CLI users

### src/main/java/com/example/pdfformfiller/PdfFormFillerCli.java(NEW)

References: 

- src/main/java/com/example/pdfformfiller/FileIOService.java(NEW)
- src/main/java/com/example/pdfformfiller/model/ReportData.java
- src/main/resources/logback.xml

Create the main CLI command class using Picocli annotations for argument parsing and command execution.

**Purpose**: Provide command-line interface for PDF form filling with proper argument validation, error handling, and exit codes.

**Class Structure** (use Lombok @Slf4j for logging):

**Picocli Annotations**:
- @Command annotation with:
  - name = "pdf-form-filler"
  - description = "Fill PDF forms from property files"
  - mixinStandardHelpOptions = true (adds --help and --version)
  - version = "1.0.0-SNAPSHOT"

**Command Options** (use Picocli @Option annotation):
- `@Option(names = {"-i", "--input"}, required = true, description = "Input PDF template file")` 
  - `private File inputFile;`
- `@Option(names = {"-p", "--properties"}, required = true, description = "Properties file with form data")`
  - `private File propertiesFile;`
- `@Option(names = {"-o", "--output"}, required = true, description = "Output PDF file path")`
  - `private File outputFile;`
- `@Option(names = {"-l", "--log"}, description = "Log file path (default: pdf-form-filler.log)")`
  - `private File logFile;`
- `@Option(names = {"-r", "--report"}, description = "Report file path (default: <output>-report.txt)")`
  - `private File reportFile;`

**Implement Callable<Integer>**:
- Override `public Integer call()` - main execution method
  - Returns exit code: 0 for success, non-zero for errors

**Main Method**:
- `public static void main(String[] args)` - entry point
  - Configure log file path BEFORE any logging (set system property)
  - Create CommandLine instance with PdfFormFillerCli
  - Execute command: `int exitCode = commandLine.execute(args);`
  - Call `System.exit(exitCode);`

**Exit Codes** (define as constants):
- `EXIT_SUCCESS = 0` - successful execution
- `EXIT_INVALID_ARGUMENTS = 1` - invalid command-line arguments
- `EXIT_FILE_NOT_FOUND = 2` - input file not found or not readable
- `EXIT_PROCESSING_ERROR = 3` - error during PDF processing
- `EXIT_IO_ERROR = 4` - I/O error (file write failure)
- `EXIT_UNEXPECTED_ERROR = 5` - unexpected exception

**call() Method Implementation**:

1. **Configure Logging** (if not already done in main):
   - If logFile is specified, set system property: `System.setProperty("log.file", logFile.getAbsolutePath());`
   - Log INFO: Application started with parameters

2. **Validate Arguments**:
   - Check inputFile exists: if not, log ERROR and return EXIT_FILE_NOT_FOUND
   - Check propertiesFile exists: if not, log ERROR and return EXIT_FILE_NOT_FOUND
   - Check inputFile is readable: if not, log ERROR and return EXIT_FILE_NOT_FOUND
   - Check propertiesFile is readable: if not, log ERROR and return EXIT_FILE_NOT_FOUND
   - Check outputFile parent directory exists or can be created: if not, log ERROR and return EXIT_IO_ERROR
   - Log DEBUG: All input validations passed

3. **Set Default Report File** (if not specified):
   - If reportFile is null: derive from outputFile name
   - Example: if output is "filled.pdf", report is "filled-report.txt"
   - Use: `reportFile = new File(outputFile.getParent(), outputFile.getName().replaceFirst("\\.[^.]+$", "-report.txt"));`
   - Log DEBUG: Using report file path

4. **Process Form**:
   - Create FileIOService instance
   - Convert File to Path: `inputFile.toPath()`, etc.
   - Call `fileIOService.processForm(inputPath, propertiesPath, outputPath)`
   - Catch IOException:
     - Log ERROR: Processing failed with exception message and stack trace
     - Return EXIT_PROCESSING_ERROR
   - Store returned ReportData
   - Log INFO: Form processing completed successfully

5. **Generate Report**:
   - Call `fileIOService.generateReport(reportData, reportPath)`
   - Catch IOException:
     - Log ERROR: Report generation failed
     - Return EXIT_IO_ERROR
   - Log INFO: Report generated successfully at report path

6. **Success**:
   - Log INFO: Application completed successfully
   - Print to console: "PDF form filled successfully. Output: <outputFile>, Report: <reportFile>"
   - Return EXIT_SUCCESS

**Exception Handling Strategy**:
- Catch specific exceptions (IOException) and return appropriate exit codes
- Catch generic Exception as fallback, log ERROR, return EXIT_UNEXPECTED_ERROR
- Never let exceptions propagate to main() - always return exit code
- Log all errors with full context (file paths, operation, exception message)

**Logging Strategy**:
- INFO: Application lifecycle (started, completed, major steps)
- DEBUG: Validation steps, derived values (default report path)
- ERROR: All failures with context
- Print user-friendly messages to System.out for success
- Print user-friendly error messages to System.err for failures

**Log File Configuration**:
- In main() method, BEFORE creating CommandLine:
  - Parse args manually to find --log/-l flag value
  - If found, set system property: `System.setProperty("log.file", logFilePath);`
  - This ensures logback picks up the custom path during initialization
  - If not found, logback uses default from logback.xml: pdf-form-filler.log

**Alternative: Configure in call() method**:
- Set system property at start of call() if logFile is specified
- Note: This may miss some early log messages from Picocli initialization
- Recommended: Configure in main() before any logging occurs

**User-Friendly Output**:
- On success: Print summary to System.out
  - "Successfully filled PDF form"
  - "Input: <inputFile>"
  - "Output: <outputFile>"
  - "Report: <reportFile>"
  - "Log: <logFile or default>"
- On error: Print error to System.err
  - "Error: <user-friendly message>"
  - "See log file for details: <logFile>"

**Imports**:
- picocli.CommandLine (CommandLine, Command, Option)
- java.io.File
- java.nio.file.Path
- java.util.concurrent.Callable
- com.example.pdfformfiller.FileIOService
- com.example.pdfformfiller.model.ReportData
- lombok.extern.slf4j.Slf4j

**Design Rationale**:
- Picocli handles argument parsing, validation, and help generation automatically
- Exit codes provide scriptable interface for automation
- Early log file configuration ensures all logs go to correct file
- FileIOService encapsulates I/O complexity
- Clear separation: CLI handles arguments/errors, FileIOService handles I/O, core components handle business logic
- User-friendly console output for interactive use
- Detailed logging for troubleshooting

### pom.xml(MODIFY)

Add Maven Shade Plugin to create an executable uber-JAR with all dependencies bundled.

**Purpose**: Enable users to run the application with a simple `java -jar pdf-form-filler.jar` command without managing classpath.

**Add Plugin** (in <build><plugins> section, after maven-jar-plugin):

**Plugin Configuration**:
- groupId: org.apache.maven.plugins
- artifactId: maven-shade-plugin
- version: 3.6.0 (latest stable)

**Executions**:
- phase: package
- goal: shade

**Configuration**:
- `<transformers>` section:
  - Add `ManifestResourceTransformer` to set Main-Class:
    - mainClass: com.example.pdfformfiller.PdfFormFillerCli
  - Add `ServicesResourceTransformer` to merge META-INF/services files (needed for SLF4J)
- `<filters>` section:
  - Exclude signature files from dependencies to avoid security exceptions:
    - Exclude: META-INF/*.SF, META-INF/*.DSA, META-INF/*.RSA
- `<finalName>`: pdf-form-filler (produces pdf-form-filler.jar instead of pdf-form-filler-1.0.0-SNAPSHOT.jar)

**Why Shade Plugin**:
- Creates single executable JAR with all dependencies
- Simplifies distribution and execution
- No need to manage classpath manually
- Standard approach for CLI applications

**Alternative Considered - Maven Assembly Plugin**:
- Also creates uber-JAR but with different approach
- Shade plugin is preferred for handling dependency conflicts and service files
- Shade plugin better handles META-INF/services merging (important for SLF4J)

**Build Output**:
- After `mvn package`, produces: target/pdf-form-filler.jar
- Can be executed with: `java -jar target/pdf-form-filler.jar --help`

**Note**: The existing maven-jar-plugin configuration can remain (it's used for non-shaded JAR), but the shaded JAR will be the primary distribution artifact.

### README.md(MODIFY)

Update README with complete usage documentation for the CLI application.

**Add/Update Sections**:

**1. Building the Project** (update existing section):
- Add: `mvn clean package` - creates executable JAR with all dependencies
- Add: Location of executable JAR: `target/pdf-form-filler.jar`
- Add: Note about uber-JAR containing all dependencies

**2. Usage** (expand existing placeholder section):

**Basic Command Syntax**:
```bash
java -jar pdf-form-filler.jar --input <template.pdf> --properties <data.properties> --output <filled.pdf>
```

**Required Flags**:
- `--input` or `-i`: Path to input PDF template file (must exist and be readable)
- `--properties` or `-p`: Path to properties file with form data (must exist and be readable)
- `--output` or `-o`: Path for output filled PDF file (parent directory must exist or will be created)

**Optional Flags**:
- `--log` or `-l`: Path to log file (default: pdf-form-filler.log in current directory)
- `--report` or `-r`: Path to processing report file (default: <output-name>-report.txt)
- `--help` or `-h`: Display help message with all options
- `--version` or `-V`: Display version information

**Example Commands**:

**Basic usage**:
```bash
java -jar pdf-form-filler.jar \
  --input template.pdf \
  --properties data.properties \
  --output filled.pdf
```

**With custom log and report paths**:
```bash
java -jar pdf-form-filler.jar \
  -i template.pdf \
  -p data.properties \
  -o output/filled.pdf \
  -l logs/processing.log \
  -r output/processing-report.txt
```

**Display help**:
```bash
java -jar pdf-form-filler.jar --help
```

**3. Exit Codes** (new section):

The application returns the following exit codes for scripting/automation:
- `0`: Success - PDF filled and report generated successfully
- `1`: Invalid arguments - check command syntax
- `2`: File not found - input PDF or properties file missing or not readable
- `3`: Processing error - error during PDF form filling (see log for details)
- `4`: I/O error - error writing output files (check permissions and disk space)
- `5`: Unexpected error - unexpected exception (see log for details)

**Example in shell script**:
```bash
java -jar pdf-form-filler.jar -i template.pdf -p data.properties -o filled.pdf
if [ $? -eq 0 ]; then
  echo "Success!"
else
  echo "Failed with exit code $?"
  exit 1
fi
```

**4. Output Files** (new section):

The application generates three output files:

**Filled PDF** (specified by --output):
- PDF form with fields filled from properties file
- Original template structure preserved
- Form fields remain editable unless flattened

**Processing Report** (specified by --report or auto-generated):
- Text file with detailed processing results
- Sections: Summary, Filled Fields, Fields Not Found, Errors, Unfilled PDF Fields
- Useful for verifying which fields were filled and troubleshooting

**Log File** (specified by --log or default):
- Detailed application log with DEBUG level information
- Includes all operations, warnings, and errors
- Essential for troubleshooting processing issues
- Rotates daily, keeps 30 days of history

**5. Property File Format** (update existing section):

Add examples showing:
- Basic properties: `firstName=Max`
- Empty values: `middleName=` (sets field to empty string)
- Multiline values: `address=Line 1\nLine 2\nLine 3`
- Special characters in field names: `field\=name=value` (field name contains `=`)
- Backslashes in values: `path=C:\\Users\\Documents` (Windows paths)
- Comments: `# This is a comment`
- Blank lines are ignored

**6. Troubleshooting** (new section):

**Common Issues**:

**"PDF does not contain an AcroForm"**:
- The input PDF is not a fillable form
- Solution: Ensure PDF has form fields (created with Adobe Acrobat, LibreOffice, etc.)

**"Property key 'X' does not match any PDF field"**:
- Property key doesn't match any field name in PDF
- Solution: Check report file for list of unfilled PDF fields, verify field names match exactly (case-sensitive)

**"Invalid option 'X' for radio button/choice field"**:
- Value not valid for radio button or dropdown field
- Solution: Check PDF field options, use exact option values

**"File not found" or "Permission denied"**:
- Input file missing or output directory not writable
- Solution: Verify file paths, check permissions, ensure parent directories exist

**7. Development** (update existing section):

Add:
- Running from source: `mvn exec:java -Dexec.mainClass="com.example.pdfformfiller.PdfFormFillerCli" -Dexec.args="--help"`
- Running tests: `mvn test`
- Building without tests: `mvn package -DskipTests`
- Viewing test coverage: (if jacoco plugin added)

**Formatting Notes**:
- Use code blocks for commands and examples
- Use bold for important terms and section headers
- Use bullet points for lists
- Include clear section separators
- Keep existing content about project description, requirements, architecture