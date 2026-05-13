package com.kurashnation.service.impl;

import com.kurashnation.dto.response.DeficitItemResponse;
import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.exception.SystemException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfGeneratorService {

    public byte[] generateRecommendationPdf(RecommendationResponse response) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            document.add(new Paragraph("Kurash Nation — Recommendation", title));
            document.add(new Paragraph("Generated: " + response.generatedDate()));
            document.add(new Paragraph("Progress: " + response.progressPercent() + "%"));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Deficits", h));
            PdfPTable table = new PdfPTable(4);
            table.addCell("Parameter");
            table.addCell("Current");
            table.addCell("Target");
            table.addCell("Diff");
            for (DeficitItemResponse d : response.deficits()) {
                table.addCell(d.parameter());
                table.addCell(String.valueOf(d.current()));
                table.addCell(String.valueOf(d.target()));
                table.addCell(String.valueOf(d.difference()));
            }
            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Week plan", h));
            for (Map.Entry<String, String> e : response.weekPlan().entrySet()) {
                document.add(new Paragraph(e.getKey() + ": " + e.getValue()));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Tip: " + response.tipOfTheDay()));

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new SystemException("PDF generation failed", ex);
        } catch (Exception ex) {
            throw new SystemException("PDF generation failed", ex);
        }
    }
}

