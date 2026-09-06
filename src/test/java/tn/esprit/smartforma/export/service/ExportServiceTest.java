package tn.esprit.smartforma.export.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.entity.Statut;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private ExcelExportService excelExportService;
    private PdfExportService pdfExportService;

    private Session testSession;
    private Formation testFormation;
    private Inscription testInscription;

    @BeforeEach
    void setUp() {
        excelExportService = new ExcelExportService();
        pdfExportService = new PdfExportService();

        Categorie cat = new Categorie();
        cat.setId(1L);
        cat.setNom("Informatique");

        testFormation = new Formation();
        testFormation.setId(100L);
        testFormation.setTitre("Spring Boot & Angular Architecture");
        testFormation.setDureeHeures(35);
        testFormation.setNiveau(Niveau.INTERMEDIAIRE);
        testFormation.setCategorie(cat);

        testSession = new Session();
        testSession.setId(500L);
        testSession.setFormation(testFormation);
        testSession.setDateDebut(LocalDate.now().plusDays(5));
        testSession.setDateFin(LocalDate.now().plusDays(10));
        testSession.setHeureDebut(LocalTime.of(9, 0));
        testSession.setHeureFin(LocalTime.of(17, 0));
        testSession.setCapacite(20);

        Apprenant apprenant = new Apprenant();
        apprenant.setId(10L);
        apprenant.setNom("Dupont");
        apprenant.setPrenom("Jean");
        apprenant.setEmail("jean.dupont@smartforma.local");
        apprenant.setNiveau(Niveau.INTERMEDIAIRE);

        testInscription = new Inscription();
        testInscription.setId(1000L);
        testInscription.setSession(testSession);
        testInscription.setApprenant(apprenant);
        testInscription.setStatut(Statut.CONFIRMEE);
        testInscription.setDateInscription(LocalDate.now());
    }

    @Test
    @DisplayName("Should generate valid Excel session attendance sheet")
    void testGenerateSessionAttendanceExcel() throws Exception {
        byte[] bytes = excelExportService.generateSessionAttendanceExcel(testSession, List.of(testInscription));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        // ZIP/XLSX magic bytes: PK (0x50, 0x4B)
        assertEquals(0x50, bytes[0]);
        assertEquals(0x4B, bytes[1]);
    }

    @Test
    @DisplayName("Should generate valid PDF session attendance sheet")
    void testGenerateSessionAttendancePdf() throws Exception {
        byte[] bytes = pdfExportService.generateSessionAttendancePdf(testSession, List.of(testInscription));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        // PDF magic bytes: %PDF (0x25, 0x50, 0x44, 0x46)
        assertEquals('%', (char) bytes[0]);
        assertEquals('P', (char) bytes[1]);
        assertEquals('D', (char) bytes[2]);
        assertEquals('F', (char) bytes[3]);
    }

    @Test
    @DisplayName("Should generate valid Excel sessions report")
    void testGenerateSessionsReportExcel() throws Exception {
        byte[] bytes = excelExportService.generateSessionsReportExcel(List.of(testSession), Map.of(testSession.getId(), 1L));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("Should generate valid PDF catalog report")
    void testGenerateCatalogReportPdf() throws Exception {
        byte[] bytes = pdfExportService.generateCatalogReportPdf(List.of(testFormation), Map.of(testFormation.getId(), 1));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        assertEquals('%', (char) bytes[0]);
    }
}
