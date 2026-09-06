package tn.esprit.smartforma.recommendation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.entity.Statut;
import tn.esprit.smartforma.inscription.repository.InscriptionRepository;
import tn.esprit.smartforma.recommendation.dto.RecommendationDto;
import tn.esprit.smartforma.recommendation.engine.RecommendationEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ApprenantRepository apprenantRepository;

    @Mock
    private FormationRepository formationRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Spy
    private RecommendationEngine recommendationEngine = new RecommendationEngine();

    @InjectMocks
    private RecommendationService recommendationService;

    private Apprenant learner;
    private Formation formationJava;
    private Formation formationCloud;
    private Formation formationManagement;

    @BeforeEach
    void setUp() {
        Categorie catTech = new Categorie();
        catTech.setId(1L);
        catTech.setNom("Informatique & Backend");

        Categorie catMgmt = new Categorie();
        catMgmt.setId(2L);
        catMgmt.setNom("Management & Agile");

        learner = new Apprenant();
        learner.setId(1L);
        learner.setNom("Doe");
        learner.setPrenom("John");
        learner.setEmail("john@example.com");
        learner.setCompetences("Java, Spring Boot");
        learner.setInterets("Backend, Microservices");
        learner.setNiveau(Niveau.INTERMEDIAIRE);

        formationJava = new Formation();
        formationJava.setId(101L);
        formationJava.setTitre("Spring Boot & REST API");
        formationJava.setDescription("Formation pratique Spring Boot");
        formationJava.setPrix(new BigDecimal("300.00"));
        formationJava.setDureeHeures(20);
        formationJava.setNiveau(Niveau.INTERMEDIAIRE);
        formationJava.setCategorie(catTech);
        formationJava.setTags("java, spring boot, rest, microservices");
        formationJava.setChapitres(new ArrayList<>());

        formationCloud = new Formation();
        formationCloud.setId(102L);
        formationCloud.setTitre("Docker & Kubernetes");
        formationCloud.setDescription("DevOps et conteneurs");
        formationCloud.setPrix(new BigDecimal("250.00"));
        formationCloud.setDureeHeures(15);
        formationCloud.setNiveau(Niveau.INTERMEDIAIRE);
        formationCloud.setCategorie(catTech);
        formationCloud.setTags("docker, kubernetes, devops, cloud");
        formationCloud.setChapitres(new ArrayList<>());

        formationManagement = new Formation();
        formationManagement.setId(103L);
        formationManagement.setTitre("Agile & Scrum Master");
        formationManagement.setDescription("Gestion de projet agile");
        formationManagement.setPrix(new BigDecimal("200.00"));
        formationManagement.setDureeHeures(10);
        formationManagement.setNiveau(Niveau.DEBUTANT);
        formationManagement.setCategorie(catMgmt);
        formationManagement.setTags("agile, scrum, sprint");
        formationManagement.setChapitres(new ArrayList<>());
    }

    @Test
    @DisplayName("9. Already confirmed formation is excluded from recommendations")
    void testAlreadyConfirmedFormationExcluded() {
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(learner));

        // Learner has a CONFIRMEE registration on formationJava (ID 101)
        Session sessionJava = new Session();
        sessionJava.setId(1L);
        sessionJava.setFormation(formationJava);

        Inscription confirmedInscription = new Inscription();
        confirmedInscription.setId(1L);
        confirmedInscription.setApprenant(learner);
        confirmedInscription.setSession(sessionJava);
        confirmedInscription.setStatut(Statut.CONFIRMEE);

        when(inscriptionRepository.findByApprenantIdOrderByDateInscriptionDesc(1L))
                .thenReturn(List.of(confirmedInscription));
        when(formationRepository.findAll()).thenReturn(List.of(formationJava, formationCloud, formationManagement));

        List<RecommendationDto> recommendations = recommendationService.recommendForLearner(1L, 4, 10);

        // formationJava should NOT be in the recommendations
        assertFalse(recommendations.stream().anyMatch(r -> r.formation().getId().equals(101L)));
        // Other formations should remain
        assertTrue(recommendations.stream().anyMatch(r -> r.formation().getId().equals(102L)));
    }

    @Test
    @DisplayName("10. Deterministic ranking — higher relevance scored courses appear first")
    void testDeterministicRanking() {
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(learner));
        when(inscriptionRepository.findByApprenantIdOrderByDateInscriptionDesc(1L)).thenReturn(List.of());
        when(formationRepository.findAll()).thenReturn(List.of(formationManagement, formationCloud, formationJava));

        List<RecommendationDto> recommendations = recommendationService.recommendForLearner(1L, 4, 10);

        assertEquals(3, recommendations.size());
        // Java formation matches skills + interests + level -> should be ranked #1
        assertEquals(101L, recommendations.get(0).formation().getId());
        // Scores must be strictly in descending order
        assertTrue(recommendations.get(0).score() >= recommendations.get(1).score());
        assertTrue(recommendations.get(1).score() >= recommendations.get(2).score());
    }

    @Test
    @DisplayName("Limit and minScore constraints are respected")
    void testLimitAndMinScore() {
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(learner));
        when(inscriptionRepository.findByApprenantIdOrderByDateInscriptionDesc(1L)).thenReturn(List.of());
        when(formationRepository.findAll()).thenReturn(List.of(formationManagement, formationCloud, formationJava));

        // Limit = 1
        List<RecommendationDto> top1 = recommendationService.recommendForLearner(1L, 1, 10);
        assertEquals(1, top1.size());
        assertEquals(101L, top1.get(0).formation().getId());

        // High minScore threshold (e.g. 80) should only return high matching courses
        List<RecommendationDto> highQuality = recommendationService.recommendForLearner(1L, 4, 80);
        for (RecommendationDto rec : highQuality) {
            assertTrue(rec.score() >= 80);
        }
    }
}
