package com.example.pdfformfiller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyParserTest {

    private Map<String, String> parseString(String content) throws IOException {
        InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return PropertyParser.parse(input);
    }

    private void assertProperty(Map<String, String> map, String key, String expectedValue) {
        assertThat(map).containsEntry(key, expectedValue);
    }

    @Nested
    class BasicParsing {
        @Test
        void testEmptyInput() throws IOException {
            assertThat(parseString("")).isEmpty();
        }

        @Test
        void testSingleProperty() throws IOException {
            Map<String, String> map = parseString("key=value");
            assertThat(map).hasSize(1);
            assertProperty(map, "key", "value");
        }

        @Test
        void testMultipleProperties() throws IOException {
            Map<String, String> map = parseString("key1=value1\nkey2=value2");
            assertThat(map).hasSize(2);
            assertProperty(map, "key1", "value1");
            assertProperty(map, "key2", "value2");
        }

        @Test
        void testEqualsSignSeparator() throws IOException {
            Map<String, String> map = parseString("key=value");
            assertProperty(map, "key", "value");
        }

        @Test
        void testColonSeparator() throws IOException {
            Map<String, String> map = parseString("key:value");
            assertProperty(map, "key", "value");
        }

        @Test
        void testWhitespaceSeparator() throws IOException {
            Map<String, String> map = parseString("key value");
            assertProperty(map, "key", "value");
        }

        @Test
        void testEmptyValue() throws IOException {
            Map<String, String> map = parseString("key=");
            assertProperty(map, "key", "");
        }

        @Test
        void testWhitespaceAroundSeparator() throws IOException {
            Map<String, String> map = parseString("key = value");
            assertProperty(map, "key", "value");
        }
    }

    @Nested
    class CommentsAndBlanks {
        @Test
        void testHashComment() throws IOException {
            assertThat(parseString("# this is a comment")).isEmpty();
        }

        @Test
        void testExclamationComment() throws IOException {
            assertThat(parseString("! this is a comment")).isEmpty();
        }

        @Test
        void testBlankLines() throws IOException {
            assertThat(parseString("\n\n")).isEmpty();
        }
    }

    @Nested
    class CustomEscapes {
        @Test
        void testEscapedEquals() throws IOException {
            Map<String, String> map = parseString("key\\=name=value");
            assertProperty(map, "key=name", "value");
        }

        @Test
        void testEscapedBackslash() throws IOException {
            Map<String, String> map = parseString("path=C:\\\\temp");
            assertProperty(map, "path", "C:\\temp");
        }

        @Test
        void testEscapedEqualsInKey() throws IOException {
            Map<String, String> map = parseString("field\\=value=data");
            assertProperty(map, "field=value", "data");
        }

        @Test
        void testMultipleEscapedBackslashes() throws IOException {
            Map<String, String> map = parseString("path\\\\to\\\\file=value");
            assertProperty(map, "path\\to\\file", "value");
        }

        @Test
        void testMixedEscapes() throws IOException {
            Map<String, String> map = parseString("a\\=b\\\\c=d\\\\e\\=f");
            assertProperty(map, "a=b\\c", "d\\e=f");
        }
    }

    @Nested
    class StandardEscapes {
        @Test
        void testTabEscape() throws IOException {
            Map<String, String> map = parseString("key=a\\tb");
            assertProperty(map, "key", "a\tb");
        }

        @Test
        void testNewlineEscape() throws IOException {
            Map<String, String> map = parseString("key=a\\nb");
            assertProperty(map, "key", "a\nb");
        }

        @Test
        void testCarriageReturnEscape() throws IOException {
            Map<String, String> map = parseString("key=a\\rb");
            assertProperty(map, "key", "a\rb");
        }

        @Test
        void testSpaceEscape() throws IOException {
            Map<String, String> map = parseString("key=a\\ b");
            assertProperty(map, "key", "a b");
        }

        @Test
        void testUnicodeEscape() throws IOException {
            Map<String, String> map = parseString("key=\\u00E9");
            assertProperty(map, "key", "é");
        }

        @Test
        void testMultipleUnicodeEscapes() throws IOException {
            Map<String, String> map = parseString("key=\\u4F60\\u597D");
            assertProperty(map, "key", "你好");
        }
    }

    @Nested
    class LineContinuation {
        @Test
        void testSimpleContinuation() throws IOException {
            Map<String, String> map = parseString("key=value1 \\\nvalue2");
            assertProperty(map, "key", "value1 value2");
        }

        @Test
        void testMultipleLineContinuation() throws IOException {
            Map<String, String> map = parseString("key=value1 \\\nvalue2 \\\nvalue3");
            assertProperty(map, "key", "value1 value2 value3");
        }

        @Test
        void testEvenBackslashesNoContinuation() throws IOException {
            Map<String, String> map = parseString("key=value1\\\\\nkey2=value2");
            assertThat(map).hasSize(2);
            assertProperty(map, "key", "value1\\");
            assertProperty(map, "key2", "value2");
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void testKeyWithDot() throws IOException {
            Map<String, String> map = parseString("user.name=value");
            assertProperty(map, "user.name", "value");
        }

        @Test
        void testKeyWithUnderscore() throws IOException {
            Map<String, String> map = parseString("user_email=value");
            assertProperty(map, "user_email", "value");
        }

        @Test
        void testKeyWithHyphen() throws IOException {
            Map<String, String> map = parseString("address-line1=value");
            assertProperty(map, "address-line1", "value");
        }

        @Test
        void testLeadingWhitespace() throws IOException {
            Map<String, String> map = parseString("  key=value");
            assertProperty(map, "key", "value");
        }

        @Test
        void testTrailingWhitespace() throws IOException {
            Map<String, String> map = parseString("key=value  ");
            assertProperty(map, "key", "value");
        }
    }
}
