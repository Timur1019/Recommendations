package com.kurashnation.util;

import com.kurashnation.exception.ValidationException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class TrainingLibraryTextExtractor {

    private TrainingLibraryTextExtractor() {
    }

    public static String extract(Path path, String contentType, String originalFilename) {
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException("Unknown content type");
        }
        String ct = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        try {
            return switch (ct) {
                case "application/pdf" -> extractPdf(path);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractDocx(path);
                case "application/msword" -> extractDoc(path);
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                     "application/vnd.ms-excel" -> extractSpreadsheet(path);
                default -> throw new ValidationException("Unsupported type for text extraction: " + ct);
            };
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            LogUtil.warn("Text extract failed for %s: %s", originalFilename, e.getMessage());
            throw new ValidationException("Could not read document text: " + e.getMessage());
        }
    }

    private static String extractPdf(Path path) throws Exception {
        PdfReader reader = new PdfReader(Files.readAllBytes(path));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                sb.append(extractor.getTextFromPage(i)).append('\n');
            }
            return normalize(sb.toString());
        } finally {
            reader.close();
        }
    }

    private static String extractDocx(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (t != null && !t.isBlank()) {
                    sb.append(t).append('\n');
                }
            }
            return normalize(sb.toString());
        }
    }

    private static String extractDoc(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); HWPFDocument doc = new HWPFDocument(in)) {
            WordExtractor ex = new WordExtractor(doc);
            return normalize(ex.getText());
        }
    }

    private static String extractSpreadsheet(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); Workbook wb = WorkbookFactory.create(in)) {
            StringBuilder sb = new StringBuilder();
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(cell.toString()).append('\t');
                    }
                    sb.append('\n');
                }
            }
            return normalize(sb.toString());
        }
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\u0000", "").trim();
    }
}
