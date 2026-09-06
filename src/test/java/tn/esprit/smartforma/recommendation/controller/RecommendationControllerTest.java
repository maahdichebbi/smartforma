package tn.esprit.smartforma.recommendation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartforma.auth.security.CurrentUserService;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;
import tn.esprit.smartforma.recommendation.dto.RecommendationDto;
import tn.esprit.smartforma.recommendation.dto.ScoreCriteriaDto;
import tn.esprit.smartforma.recommendation.service.RecommendationService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RecommendationController recommendationController;

    @Test
    @DisplayName("GET /api/v1/recommendations/apprenant/{id} returns recommendations list with explanations")
    void testGetRecommendations() {
        Categorie cat = new Categorie();
        cat.setId(1L);
        cat.setNom("Backend");

        Formation f = new Formation();
        f.setId(1L);
        f.setTitre("Spring Boot Masterclass");
        f.setDescription("Spring Boot cours");
        f.setPrix(new BigDecimal("200.00"));
        f.setDureeHeures(20);
        f.setNiveau(Niveau.INTERMEDIAIRE);
        f.setCategorie(cat);

        RecommendationDto rec = new RecommendationDto(
                f,
                92,
                List.of("Compétences correspondantes : Java, Spring Boot (+40%)", "Niveau Intermédiaire adapté (+25%)"),
                new ScoreCriteriaDto(100.0, 75.0, 100.0)
        );

        when(recommendationService.recommendForLearner(1L, 4, 20))
                .thenReturn(List.of(rec));

        List<RecommendationDto> results = recommendationController.getRecommendationsForLearner(1L, 4, 20);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Spring Boot Masterclass", results.get(0).formation().getTitre());
        assertEquals(92, results.get(0).score());
        assertEquals(2, results.get(0).explications().size());
        assertEquals(100.0, results.get(0).criteres().scoreCompetences());

        verify(recommendationService).recommendForLearner(1L, 4, 20);
        verify(currentUserService).assertCanAccessApprenant(1L);
    }
}
