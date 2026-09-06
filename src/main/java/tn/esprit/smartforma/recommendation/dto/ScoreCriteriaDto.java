package tn.esprit.smartforma.recommendation.dto;

/**
 * Score breakdown across the 3 dimensions evaluated by the MLA engine.
 */
public record ScoreCriteriaDto(
        double scoreCompetences,
        double scoreInterets,
        double scoreNiveau
) {}
