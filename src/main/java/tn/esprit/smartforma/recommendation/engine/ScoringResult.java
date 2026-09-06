package tn.esprit.smartforma.recommendation.engine;

import tn.esprit.smartforma.recommendation.dto.ScoreCriteriaDto;

import java.util.List;

/**
 * Internal result produced by the RecommendationEngine.
 */
public record ScoringResult(
        int finalScore,
        double skillsScore,
        double interestsScore,
        double levelScore,
        List<String> matchedSkills,
        List<String> matchedInterests,
        List<String> explications
) {
    public ScoreCriteriaDto toCriteriaDto() {
        return new ScoreCriteriaDto(
                Math.round(skillsScore * 10.0) / 10.0,
                Math.round(interestsScore * 10.0) / 10.0,
                Math.round(levelScore * 10.0) / 10.0
        );
    }
}
