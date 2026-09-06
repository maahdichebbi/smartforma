package tn.esprit.smartforma.export.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Brand Colors
    private static final Color COLOR_PRIMARY = new Color(67, 56, 202);     // #4338CA Indigo
    private static final Color COLOR_HEADER_BG = new Color(30, 41, 59);    // #1E293B Slate 800
    private static final Color COLOR_ZEBRA = new Color(248, 250, 252);     // #F8FAFC
    private static final Color COLOR_BORDER = new Color(226, 232, 240);    // #E2E8F0
    private static final Color COLOR_TEXT = new Color(30, 41, 59);         // Slate 800
    private static final Color COLOR_MUTED = new Color(100, 116, 139);     // Slate 500

    // Fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXT);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);

    /**
     * Generates a printable A4 Landscape session attendance sheet (Feuille d'émargement).
     */
    public byte[] generateSessionAttendancePdf(Session session, List<Inscription> inscriptions) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // 1. Header Banner
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{70, 30});

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.addElement(new Paragraph("SMARTFORMA — FEUILLE D'ÉMARGEMENT", FONT_TITLE));
        titleCell.addElement(new Paragraph("Registre officiel de présence pour session de formation", FONT_SUBTITLE));
        headerTable.addCell(titleCell);

        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pDate = new Paragraph("Généré le : " + LocalDate.now().format(DATE_FMT) + "\nSession ID : #" + session.getId(), FONT_SUBTITLE);
        pDate.setAlignment(Element.ALIGN_RIGHT);
        dateCell.addElement(pDate);
        headerTable.addCell(dateCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));

        // 2. Session Metadata Box
        Formation f = session.getFormation();
        String formationTitre = (f != null) ? f.getTitre() : "Formation #" + session.getId();
        String catNom = (f != null && f.getCategorie() != null) ? f.getCategorie().getNom() : "Non catégorisée";
        int duree = (f != null && f.getDureeHeures() != null) ? f.getDureeHeures() : 0;

        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{25, 25, 25, 25});

        addMetaBoxCell(metaTable, "Formation", formationTitre);
        addMetaBoxCell(metaTable, "Catégorie / Durée", catNom + " (" + duree + "h)");
        addMetaBoxCell(metaTable, "Dates", formatDate(session.getDateDebut()) + " → " + formatDate(session.getDateFin()));
        addMetaBoxCell(metaTable, "Horaires & Capacité",
                (session.getHeureDebut() != null ? session.getHeureDebut().toString() : "00h00")
                        + " - " + (session.getHeureFin() != null ? session.getHeureFin().toString() : "Fin")
                        + " (" + session.getCapacite() + " places)");

        document.add(metaTable);
        document.add(new Paragraph(" "));

        // 3. Attendance Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 24, 25, 12, 11, 11, 11});

        String[] columnHeaders = {
                "N°", "Apprenant", "Email", "Inscrit le", "Statut",
                "Matin (Visa)", "Après-midi (Visa)"
        };

        for (String col : columnHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(col, FONT_TH));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBorderColor(COLOR_BORDER);
            table.addCell(cell);
        }

        int counter = 1;
        boolean zebra = false;
        for (Inscription ins : inscriptions) {
            Color rowBg = zebra ? COLOR_ZEBRA : Color.WHITE;
            zebra = !zebra;

            Apprenant app = ins.getApprenant();
            String appNom = (app != null) ? (app.getNom() + " " + app.getPrenom()) : "N/A";
            String appEmail = (app != null) ? app.getEmail() : "N/A";

            // N°
            addTableCell(table, String.valueOf(counter++), FONT_TD, Element.ALIGN_CENTER, rowBg);
            // Nom
            addTableCell(table, appNom, FONT_TD_BOLD, Element.ALIGN_LEFT, rowBg);
            // Email
            addTableCell(table, appEmail, FONT_TD, Element.ALIGN_LEFT, rowBg);
            // Date
            addTableCell(table, ins.getDateInscription() != null ? ins.getDateInscription().format(DATE_FMT) : "-", FONT_TD, Element.ALIGN_CENTER, rowBg);
            // Statut
            addTableCell(table, ins.getStatut() != null ? ins.getStatut().name() : "-", FONT_TD, Element.ALIGN_CENTER, rowBg);
            // Matin Signature Box (empty)
            addTableCell(table, "", FONT_TD, Element.ALIGN_CENTER, rowBg);
            // Apres-midi Signature Box (empty)
            addTableCell(table, "", FONT_TD, Element.ALIGN_CENTER, rowBg);
        }

        if (inscriptions.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Aucun apprenant enregistré pour cette session.", FONT_TD));
            emptyCell.setColspan(7);
            emptyCell.setPadding(12);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // 4. Instructor Signature Box
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{50, 50});

        PdfPCell sign1 = new PdfPCell();
        sign1.setBorder(Rectangle.BOX);
        sign1.setBorderColor(COLOR_BORDER);
        sign1.setPadding(10);
        sign1.addElement(new Paragraph("Visa & Signature du Formateur :", FONT_SECTION));
        sign1.addElement(new Paragraph("\n\nDate : ..................................", FONT_SMALL));
        signTable.addCell(sign1);

        PdfPCell sign2 = new PdfPCell();
        sign2.setBorder(Rectangle.BOX);
        sign2.setBorderColor(COLOR_BORDER);
        sign2.setPadding(10);
        sign2.addElement(new Paragraph("Visa de la Direction Pédagogique :", FONT_SECTION));
        sign2.addElement(new Paragraph("\n\nCachet et signature : ..................................", FONT_SMALL));
        signTable.addCell(sign2);

        document.add(signTable);

        document.close();
        return out.toByteArray();
    }

    /**
     * Generates a comprehensive PDF report for the training catalog and statistics.
     */
    public byte[] generateCatalogReportPdf(List<Formation> formations, Map<Long, Integer> sessionCounts) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Header
        Paragraph title = new Paragraph("SMARTFORMA — RAPPORT GLOBAL DU CATALOGUE", FONT_TITLE);
        Paragraph subtitle = new Paragraph("Synthèse générale des formations, catégories et sessions planifiées\nÉdité le : "
                + LocalDate.now().format(DATE_FMT) + "\n\n", FONT_SUBTITLE);
        document.add(title);
        document.add(subtitle);

        // Catalog Table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 32, 22, 14, 12, 12});

        String[] headers = {"ID", "Formation", "Catégorie", "Niveau", "Durée", "Sessions"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_TH));
            c.setBackgroundColor(COLOR_HEADER_BG);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setBorderColor(COLOR_BORDER);
            table.addCell(c);
        }

        boolean zebra = false;
        for (Formation f : formations) {
            Color rowBg = zebra ? COLOR_ZEBRA : Color.WHITE;
            zebra = !zebra;

            int sessCount = sessionCounts.getOrDefault(f.getId(), 0);
            String catNom = f.getCategorie() != null ? f.getCategorie().getNom() : "Non classée";
            String niveau = f.getNiveau() != null ? f.getNiveau().name() : "DEBUTANT";
            int duree = f.getDureeHeures() != null ? f.getDureeHeures() : 0;

            addTableCell(table, "#" + f.getId(), FONT_TD, Element.ALIGN_CENTER, rowBg);
            addTableCell(table, f.getTitre(), FONT_TD_BOLD, Element.ALIGN_LEFT, rowBg);
            addTableCell(table, catNom, FONT_TD, Element.ALIGN_LEFT, rowBg);
            addTableCell(table, niveau, FONT_TD, Element.ALIGN_CENTER, rowBg);
            addTableCell(table, duree + " h", FONT_TD, Element.ALIGN_CENTER, rowBg);
            addTableCell(table, String.valueOf(sessCount), FONT_TD_BOLD, Element.ALIGN_CENTER, rowBg);
        }

        document.add(table);

        document.close();
        return out.toByteArray();
    }

    // --- Helpers ---

    private void addMetaBoxCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_ZEBRA);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph(label, FONT_SMALL));
        cell.addElement(new Paragraph(value, FONT_TD_BOLD));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }

    private String formatDate(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "N/A";
    }
}
