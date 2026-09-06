package tn.esprit.smartforma.export.controller;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;
import tn.esprit.smartforma.export.service.ExcelExportService;
import tn.esprit.smartforma.export.service.PdfExportService;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.entity.Statut;
import tn.esprit.smartforma.inscription.repository.InscriptionRepository;
import tn.esprit.smartforma.inscription.repository.SessionRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final SessionRepository sessionRepository;
    private final InscriptionRepository inscriptionRepository;
    private final FormationRepository formationRepository;
    private final ApprenantRepository apprenantRepository;

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Download printable session attendance sheet in PDF format.
     */
    @GetMapping("/sessions/{sessionId}/attendance/pdf")
    public ResponseEntity<byte[]> exportSessionAttendancePdf(@PathVariable Long sessionId) throws DocumentException {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'identifiant " + sessionId));

        List<Inscription> inscriptions = inscriptionRepository.findBySessionId(sessionId);
        byte[] pdfBytes = pdfExportService.generateSessionAttendancePdf(session, inscriptions);

        String filename = "emargement-session-" + sessionId + "-" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Download session attendance sheet in Excel format.
     */
    @GetMapping("/sessions/{sessionId}/attendance/excel")
    public ResponseEntity<byte[]> exportSessionAttendanceExcel(@PathVariable Long sessionId) throws IOException {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session non trouvée avec l'identifiant " + sessionId));

        List<Inscription> inscriptions = inscriptionRepository.findBySessionId(sessionId);
        byte[] excelBytes = excelExportService.generateSessionAttendanceExcel(session, inscriptions);

        String filename = "emargement-session-" + sessionId + "-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }

    /**
     * Download global sessions planning and occupancy spreadsheet in Excel format.
     */
    @GetMapping("/sessions/excel")
    public ResponseEntity<byte[]> exportSessionsExcel() throws IOException {
        List<Session> sessions = sessionRepository.findAll();

        Map<Long, Long> activeCounts = new HashMap<>();
        for (Session s : sessions) {
            activeCounts.put(s.getId(), sessionRepository.countActiveInscriptions(s.getId()));
        }

        byte[] excelBytes = excelExportService.generateSessionsReportExcel(sessions, activeCounts);
        String filename = "planning-sessions-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }

    /**
     * Download global registrations list in Excel format.
     */
    @GetMapping("/inscriptions/excel")
    public ResponseEntity<byte[]> exportInscriptionsExcel(@RequestParam(required = false) Statut statut) throws IOException {
        List<Inscription> list;
        if (statut != null) {
            list = inscriptionRepository.findAll().stream()
                    .filter(i -> i.getStatut() == statut)
                    .toList();
        } else {
            list = inscriptionRepository.findAll();
        }

        byte[] excelBytes = excelExportService.generateInscriptionsExcel(list);
        String filename = "registre-inscriptions-" + (statut != null ? statut.name().toLowerCase() + "-" : "") + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }

    /**
     * Download registered students roster in Excel format.
     */
    @GetMapping("/apprenants/excel")
    public ResponseEntity<byte[]> exportApprenantsExcel() throws IOException {
        List<Apprenant> apprenants = apprenantRepository.findAll();

        Map<Long, Long> counts = new HashMap<>();
        List<Inscription> allInscriptions = inscriptionRepository.findAll();
        for (Inscription ins : allInscriptions) {
            if (ins.getApprenant() != null) {
                Long appIId = ins.getApprenant().getId();
                counts.put(appIId, counts.getOrDefault(appIId, 0L) + 1);
            }
        }

        byte[] excelBytes = excelExportService.generateApprenantsExcel(apprenants, counts);
        String filename = "roster-apprenants-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }

    /**
     * Download training catalog summary report in PDF format.
     */
    @GetMapping("/formations/catalog/pdf")
    public ResponseEntity<byte[]> exportCatalogPdf() throws DocumentException {
        List<Formation> formations = formationRepository.findAll();
        List<Session> allSessions = sessionRepository.findAll();

        Map<Long, Integer> sessionCounts = new HashMap<>();
        for (Session s : allSessions) {
            if (s.getFormation() != null) {
                Long fId = s.getFormation().getId();
                sessionCounts.put(fId, sessionCounts.getOrDefault(fId, 0) + 1);
            }
        }

        byte[] pdfBytes = pdfExportService.generateCatalogReportPdf(formations, sessionCounts);
        String filename = "rapport-catalogue-" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
