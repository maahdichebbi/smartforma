package tn.esprit.smartforma.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;
import tn.esprit.smartforma.inscription.entity.Statut;
import tn.esprit.smartforma.inscription.repository.InscriptionRepository;
import tn.esprit.smartforma.recommendation.dto.RecommendationDto;
import tn.esprit.smartforma.recommendation.engine.RecommendationEngine;
import tn.esprit.smartforma.recommendation.engine.ScoringResult;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service orchestrating data retrieval, learner history exclusion, candidate scoring,
 * and top-K ranked recommendation generation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final ApprenantRepository apprenantRepository;
    private final FormationRepository formationRepository;
    private final InscriptionRepository inscriptionRepository;
    private final RecommendationEngine recommendationEngine;

    /**
     * Computes ranked recommendations for a given learner.
     *
     * @param apprenantId Target learner ID
     * @param limit       Maximum number of recommendations to return (default 4)
     * @param minScore    Minimum relevance score threshold (default 20)
     * @return Ranked list of RecommendationDto sorted descending by score
     */
    public List<RecommendationDto> recommendForLearner(Long apprenantId, int limit, int minScore) {
        Apprenant learner = apprenantRepository.findById(apprenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant", apprenantId));

        // 1. Identify formations where learner already has an active (CONFIRMEE) registration
        Set<Long> excludedFormationIds = inscriptionRepository.findByApprenantIdOrderByDateInscriptionDesc(apprenantId)
                .stream()
                .filter(i -> i.getStatut() == Statut.CONFIRMEE && i.getSession() != null && i.getSession().getFormation() != null)
                .map(i -> i.getSession().getFormation().getId())
                .collect(Collectors.toSet());

        // 2. Fetch all candidate formations
        List<Formation> allFormations = formationRepository.findAll();

        // 3. Score and rank candidates
        return allFormations.stream()
                .filter(formation -> !excludedFormationIds.contains(formation.getId()))
                .map(formation -> {
                    ScoringResult result = recommendationEngine.evaluate(learner, formation);
                    return new RecommendationDto(
                            formation,
                            result.finalScore(),
                            result.explications(),
                            result.toCriteriaDto()
                    );
                })
                .filter(rec -> rec.score() >= minScore)
                .sorted(Comparator.comparingInt(RecommendationDto::score).reversed()
                        .thenComparing(rec -> rec.formation().getTitre(), String.CASE_INSENSITIVE_ORDER))
                .limit(limit > 0 ? limit : 4)
                .toList();
    }
}
