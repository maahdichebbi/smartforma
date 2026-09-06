package tn.esprit.smartforma.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.auth.security.CurrentUserService;
import tn.esprit.smartforma.recommendation.dto.RecommendationDto;
import tn.esprit.smartforma.recommendation.service.RecommendationService;

import java.util.List;

/**
 * REST controller exposing the MLA recommendation engine.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    /**
     * Get personalized, explainable recommendations for a specific learner.
     *
     * GET /api/v1/recommendations/apprenant/{apprenantId}?limit=4&minScore=20
     */
    @GetMapping("/apprenant/{apprenantId}")
    public List<RecommendationDto> getRecommendationsForLearner(
            @PathVariable Long apprenantId,
            @RequestParam(defaultValue = "4") int limit,
            @RequestParam(defaultValue = "20") int minScore
    ) {
        currentUserService.assertCanAccessApprenant(apprenantId);
        return recommendationService.recommendForLearner(apprenantId, limit, minScore);
    }
}
