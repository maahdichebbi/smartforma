package tn.esprit.smartforma.recommendation.dto;

import tn.esprit.smartforma.catalogue.entity.Formation;

import java.util.List;

/**
 * Structured recommendation item returned by the MLA API.
 */
public record RecommendationDto(
        Formation formation,
        int score,
        List<String> explications,
        ScoreCriteriaDto criteres
) {}
