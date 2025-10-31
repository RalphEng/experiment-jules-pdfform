package com.example.pdfformfiller;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class PropertyParser {

    public static Map<String, String> parse(InputStream input) throws IOException {
        log.debug("Starting property parsing.");
        Map<String, String> properties = new LinkedHashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        StringBuilder rawLine = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if (rawLine.length() > 0) {
                rawLine.setLength(0);
            }
            rawLine.append(line);
            boolean isMultiLine = isMultiLine(line);
            while (isMultiLine) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                rawLine.setLength(rawLine.length() - 1);
                rawLine.append(line.trim());
                isMultiLine = isMultiLine(line);
            }

            String currentLine = rawLine.toString();
            int len = currentLine.length();
            int keyStart;
            for (keyStart = 0; keyStart < len; keyStart++) {
                if (!Character.isWhitespace(currentLine.charAt(keyStart))) {
                    break;
                }
            }

            if (keyStart == len || currentLine.charAt(keyStart) == '#' || currentLine.charAt(keyStart) == '!') {
                continue;
            }

            int separatorIndex;
            for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++) {
                char c = currentLine.charAt(separatorIndex);
                if (c == '\\') {
                    separatorIndex++;
                } else if (c == '=' || c == ':' || Character.isWhitespace(c)) {
                    break;
                }
            }

            String key = currentLine.substring(keyStart, separatorIndex);

            int valueIndex;
            for (valueIndex = separatorIndex; valueIndex < len; valueIndex++) {
                if (!Character.isWhitespace(currentLine.charAt(valueIndex))) {
                    break;
                }
            }

            if (valueIndex < len && (currentLine.charAt(valueIndex) == '=' || currentLine.charAt(valueIndex) == ':')) {
                valueIndex++;
                for (; valueIndex < len; valueIndex++) {
                    if (!Character.isWhitespace(currentLine.charAt(valueIndex))) {
                        break;
                    }
                }
            }

            String value = (valueIndex < len) ? currentLine.substring(valueIndex) : "";

            properties.put(unescape(key), unescape(value.trim()));
            log.debug("Parsed property: key='{}', value='{}'", unescape(key), unescape(value));
        }
        log.info("Property parsing complete. Found {} properties.", properties.size());
        return properties;
    }

    private static boolean isMultiLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        int backslashCount = 0;
        for (int i = line.length() - 1; i >= 0; i--) {
            if (line.charAt(i) == '\\') {
                backslashCount++;
            } else {
                break;
            }
        }
        return backslashCount % 2 != 0;
    }

    private static int findSeparator(String line) {
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                i++; // Skip next character
            } else if (c == '=' || c == ':' || Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String str) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case '=':
                        sb.append('=');
                        i++;
                        break;
                    case ':':
                        sb.append(':');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 'r':
                        sb.append('\r');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case ' ':
                        sb.append(' ');
                        i++;
                        break;
                    case '#':
                        sb.append('#');
                        i++;
                        break;
                    case '!':
                        sb.append('!');
                        i++;
                        break;
                    case 'u':
                        if (i + 5 < len) {
                            try {
                                String hex = str.substring(i + 2, i + 6);
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default:
                        sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
