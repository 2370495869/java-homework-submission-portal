package io.github.homeworkportal.storage;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class FileTypeValidatorTest {
    @Test
    void acceptsSupportedSignaturesAndSanitizesClientPath() throws Exception {
        var pdf = FileTypeValidator.validate("C:\\fakepath\\assignment.PDF",
                new ByteArrayInputStream("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("assignment.PDF", pdf.filename());
        assertEquals(".pdf", pdf.extension());

        byte[] doc = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        assertEquals(".doc", FileTypeValidator.validate("report.doc", new ByteArrayInputStream(doc)).extension());

        byte[] docx = {'P', 'K', 3, 4, 0, 0, 0, 0};
        assertEquals(".docx", FileTypeValidator.validate("report.docx", new ByteArrayInputStream(docx)).extension());
    }

    @Test
    void rejectsUnknownTypesAndMismatchedContent() {
        assertThrows(IllegalArgumentException.class, () -> FileTypeValidator.validate(
                "notes.txt", new ByteArrayInputStream("plain text".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> FileTypeValidator.validate(
                "fake.pdf", new ByteArrayInputStream("plain text".getBytes(StandardCharsets.UTF_8))));
        assertEquals("", FileTypeValidator.sanitizeFilename("../"));
    }
}
