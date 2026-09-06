package tn.esprit.smartforma.export.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Generates a styled Excel attendance sheet for a specific session.
     */
    public byte[] generateSessionAttendanceExcel(Session session, List<Inscription> inscriptions) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Émargement Session " + session.getId());
            sheet.setDisplayGridlines(true);

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle metaLabelStyle = createBoldStyle(workbook, IndexedColors.GREY_50_PERCENT.getIndex());
            CellStyle metaValueStyle = createNormalStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            // Title block
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SMARTFORMA — FEUILLE D'ÉMARGEMENT ET DE PRÉSENCE");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Metadata block
            Formation f = session.getFormation();
            String formationTitre = (f != null) ? f.getTitre() : "Formation #" + session.getId();
            String catName = (f != null && f.getCategorie() != null) ? f.getCategorie().getNom() : "Non catégorisée";
            int duree = (f != null && f.getDureeHeures() != null) ? f.getDureeHeures() : 0;

            createMetaRow(sheet, 2, "Formation :", formationTitre + " (" + catName + " — " + duree + "h)", metaLabelStyle, metaValueStyle);
            createMetaRow(sheet, 3, "Période :", formatDate(session.getDateDebut()) + " au " + formatDate(session.getDateFin()), metaLabelStyle, metaValueStyle);
            createMetaRow(sheet, 4, "Horaires :", (session.getHeureDebut() != null ? session.getHeureDebut().toString() : "N/A") + " - " + (session.getHeureFin() != null ? session.getHeureFin().toString() : "N/A"), metaLabelStyle, metaValueStyle);
            createMetaRow(sheet, 5, "Capacité :", session.getCapacite() + " places", metaLabelStyle, metaValueStyle);

            // Table headers
            String[] headers = {"N°", "Nom & Prénom", "Email", "Date Inscription", "Statut", "Émargement / Présence"};
            Row headerRow = sheet.createRow(7);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 8;
            int counter = 1;
            for (Inscription ins : inscriptions) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                Cell cNum = row.createCell(0);
                cNum.setCellValue(counter++);
                cNum.setCellStyle(centerStyle);

                Apprenant app = ins.getApprenant();
                String appNom = (app != null) ? (app.getNom() + " " + app.getPrenom()) : "N/A";
                String appEmail = (app != null) ? app.getEmail() : "N/A";

                Cell cNom = row.createCell(1);
                cNom.setCellValue(appNom);
                cNom.setCellStyle(dataStyle);

                Cell cEmail = row.createCell(2);
                cEmail.setCellValue(appEmail);
                cEmail.setCellStyle(dataStyle);

                Cell cDate = row.createCell(3);
                cDate.setCellValue(ins.getDateInscription() != null ? ins.getDateInscription().format(DATE_FMT) : "");
                cDate.setCellStyle(centerStyle);

                Cell cStatut = row.createCell(4);
                cStatut.setCellValue(ins.getStatut() != null ? ins.getStatut().name() : "");
                cStatut.setCellStyle(centerStyle);

                Cell cSig = row.createCell(5);
                cSig.setCellValue("");
                cSig.setCellStyle(dataStyle);
            }

            if (inscriptions.isEmpty()) {
                Row emptyRow = sheet.createRow(rowIdx);
                Cell cell = emptyRow.createCell(0);
                cell.setCellValue("Aucun apprenant inscrit à cette session pour le moment.");
                cell.setCellStyle(metaValueStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 5));
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3500));
            }
            sheet.setColumnWidth(5, 7500); // larger signature column

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates a complete Excel overview of all sessions and their occupancy rates.
     */
    public byte[] generateSessionsReportExcel(List<Session> sessions, Map<Long, Long> activeCountMap) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Planning Sessions");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SMARTFORMA — SYNTHÈSE GLOBALE DES SESSIONS DE FORMATION");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            String[] headers = {
                    "ID Session", "Formation", "Catégorie", "Date Début", "Date Fin",
                    "Horaires", "Inscrits Confirmés", "Capacité", "Taux d'occupation (%)"
            };

            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (Session s : sessions) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                Formation f = s.getFormation();
                long activeCount = activeCountMap.getOrDefault(s.getId(), 0L);
                int capacite = (s.getCapacite() != null && s.getCapacite() > 0) ? s.getCapacite() : 1;
                double taux = ((double) activeCount / capacite) * 100.0;

                Cell cId = row.createCell(0);
                cId.setCellValue("#" + s.getId());
                cId.setCellStyle(centerStyle);

                Cell cTitre = row.createCell(1);
                cTitre.setCellValue(f != null ? f.getTitre() : "N/A");
                cTitre.setCellStyle(dataStyle);

                Cell cCat = row.createCell(2);
                cCat.setCellValue((f != null && f.getCategorie() != null) ? f.getCategorie().getNom() : "Sans catégorie");
                cCat.setCellStyle(dataStyle);

                Cell cDebut = row.createCell(3);
                cDebut.setCellValue(formatDate(s.getDateDebut()));
                cDebut.setCellStyle(centerStyle);

                Cell cFin = row.createCell(4);
                cFin.setCellValue(formatDate(s.getDateFin()));
                cFin.setCellStyle(centerStyle);

                Cell cHoraires = row.createCell(5);
                cHoraires.setCellValue((s.getHeureDebut() != null ? s.getHeureDebut().toString() : "00:00")
                        + " - " + (s.getHeureFin() != null ? s.getHeureFin().toString() : "Fin"));
                cHoraires.setCellStyle(centerStyle);

                Cell cInscrits = row.createCell(6);
                cInscrits.setCellValue(activeCount);
                cInscrits.setCellStyle(centerStyle);

                Cell cCap = row.createCell(7);
                cCap.setCellValue(s.getCapacite());
                cCap.setCellStyle(centerStyle);

                Cell cTaux = row.createCell(8);
                cTaux.setCellValue(String.format("%.1f %%", taux));
                cTaux.setCellStyle(centerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates an Excel report of all registrations.
     */
    public byte[] generateInscriptionsExcel(List<Inscription> inscriptions) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inscriptions");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SMARTFORMA — REGISTRE GLOBAL DES INSCRIPTIONS");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            String[] headers = {
                    "ID Inscription", "Date", "Apprenant", "Email",
                    "Formation", "Session", "Statut", "Position File"
            };

            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (Inscription ins : inscriptions) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                Apprenant app = ins.getApprenant();
                Session sess = ins.getSession();
                Formation f = (sess != null) ? sess.getFormation() : null;

                Cell cId = row.createCell(0);
                cId.setCellValue("#" + ins.getId());
                cId.setCellStyle(centerStyle);

                Cell cDate = row.createCell(1);
                cDate.setCellValue(ins.getDateInscription() != null ? ins.getDateInscription().format(DATE_FMT) : "");
                cDate.setCellStyle(centerStyle);

                Cell cNom = row.createCell(2);
                cNom.setCellValue(app != null ? (app.getNom() + " " + app.getPrenom()) : "N/A");
                cNom.setCellStyle(dataStyle);

                Cell cEmail = row.createCell(3);
                cEmail.setCellValue(app != null ? app.getEmail() : "N/A");
                cEmail.setCellStyle(dataStyle);

                Cell cFormation = row.createCell(4);
                cFormation.setCellValue(f != null ? f.getTitre() : "N/A");
                cFormation.setCellStyle(dataStyle);

                Cell cSession = row.createCell(5);
                cSession.setCellValue(sess != null ? ("#" + sess.getId() + " (" + formatDate(sess.getDateDebut()) + ")") : "N/A");
                cSession.setCellStyle(centerStyle);

                Cell cStatut = row.createCell(6);
                cStatut.setCellValue(ins.getStatut() != null ? ins.getStatut().name() : "");
                cStatut.setCellStyle(centerStyle);

                Cell cFile = row.createCell(7);
                cFile.setCellValue(ins.getPositionFile() != null ? ins.getPositionFile().toString() : "-");
                cFile.setCellStyle(centerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates an Excel roster of registered learners.
     */
    public byte[] generateApprenantsExcel(List<Apprenant> apprenants, Map<Long, Long> inscriptionsCountMap) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Apprenants");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createBorderedStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("SMARTFORMA — ROSTER GLOBAL DES APPRENANTS");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            String[] headers = {
                    "ID", "Nom", "Prénom", "Email", "Niveau", "Compétences", "Intérêts", "Total Inscriptions"
            };

            Row headerRow = sheet.createRow(2);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (Apprenant app : apprenants) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                long count = inscriptionsCountMap.getOrDefault(app.getId(), 0L);

                Cell cId = row.createCell(0);
                cId.setCellValue("#" + app.getId());
                cId.setCellStyle(centerStyle);

                Cell cNom = row.createCell(1);
                cNom.setCellValue(app.getNom() != null ? app.getNom() : "");
                cNom.setCellStyle(dataStyle);

                Cell cPrenom = row.createCell(2);
                cPrenom.setCellValue(app.getPrenom() != null ? app.getPrenom() : "");
                cPrenom.setCellStyle(dataStyle);

                Cell cEmail = row.createCell(3);
                cEmail.setCellValue(app.getEmail() != null ? app.getEmail() : "");
                cEmail.setCellStyle(dataStyle);

                Cell cNiveau = row.createCell(4);
                cNiveau.setCellValue(app.getNiveau() != null ? app.getNiveau().name() : "DEBUTANT");
                cNiveau.setCellStyle(centerStyle);

                Cell cComp = row.createCell(5);
                cComp.setCellValue(app.getCompetences() != null ? app.getCompetences() : "-");
                cComp.setCellStyle(dataStyle);

                Cell cInt = row.createCell(6);
                cInt.setCellValue(app.getInterets() != null ? app.getInterets() : "-");
                cInt.setCellStyle(dataStyle);

                Cell cCount = row.createCell(7);
                cCount.setCellValue(count);
                cCount.setCellStyle(centerStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // --- Helper Style Methods ---

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb, short colorIndex) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(colorIndex);
        style.setFont(font);
        return style;
    }

    private CellStyle createNormalStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private void createMetaRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell cLabel = row.createCell(0);
        cLabel.setCellValue(label);
        cLabel.setCellStyle(labelStyle);

        Cell cVal = row.createCell(1);
        cVal.setCellValue(value);
        cVal.setCellStyle(valueStyle);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "N/A";
    }
}
