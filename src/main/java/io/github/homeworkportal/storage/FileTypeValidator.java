package io.github.homeworkportal.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class FileTypeValidator {
    private static final byte[] OLE_SIGNATURE = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
        (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private FileTypeValidator() {
    }

    public static ValidatedUpload validate(String suppliedName, InputStream input) throws IOException {
        String filename = sanitizeFilename(suppliedName);
        if (filename.isBlank()) {
            throw new IllegalArgumentException("文件名无效。");
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        String extension;
        String mediaType;
        if (lower.endsWith(".pdf")) {
            extension = ".pdf";
            mediaType = "application/pdf";
        } else if (lower.endsWith(".docx")) {
            extension = ".docx";
            mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lower.endsWith(".doc")) {
            extension = ".doc";
            mediaType = "application/msword";
        } else {
            throw new IllegalArgumentException("仅支持 PDF、DOC 和 DOCX 文件。");
        }

        byte[] header = input.readNBytes(8);
        boolean signatureMatches = switch (extension) {
            case ".pdf" -> startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case ".doc" -> startsWith(header, OLE_SIGNATURE);
            case ".docx" -> header.length >= 4 && header[0] == 'P' && header[1] == 'K'
                    && header[2] == 3 && header[3] == 4;
            default -> false;
        };
        if (!signatureMatches) {
            throw new IllegalArgumentException("文件内容与扩展名不匹配。");
        }
        return new ValidatedUpload(filename, extension, mediaType);
    }

    public static String sanitizeFilename(String supplied) {
        if (supplied == null) {
            return "";
        }
        String normalized = supplied.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}<>:\"|?*]", "_")
                .replaceAll("[. ]+$", "")
                .trim();
        filename = filename.replaceAll("^\\.+", "_");
        if (filename.isBlank() || filename.equals(".") || filename.equals("..")) {
            return "";
        }
        if (filename.length() > 180) {
            filename = filename.substring(filename.length() - 180);
        }
        return filename;
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
