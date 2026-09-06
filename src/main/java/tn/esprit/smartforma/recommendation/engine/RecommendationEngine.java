package tn.esprit.smartforma.recommendation.engine;

import org.springframework.stereotype.Component;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;

import java.util.*;

/**
 * Core mathematical engine for Content-Based recommendation and multi-criteria scoring.
 *
 * Implements:
 * 1. Skills Overlap Scoring (Weight: 40%)
 * 2. Interests & Domain Alignment Scoring (Weight: 35%)
 * 3. Pedagogical Level Compatibility Matrix (Weight: 25%)
 * 4. Human-readable explainability generator (XAI)
 */
@Component
public class RecommendationEngine {

    public static final double WEIGHT_SKILLS = 0.40;
    public static final double WEIGHT_INTERESTS = 0.35;
    public static final double WEIGHT_LEVEL = 0.25;

    /**
     * Evaluates the affinity between a learner profile and a target formation.
     *
     * @param learner   The learner profile
     * @param formation The formation candidate
     * @return Deterministic scoring result with breakdowns and explanations
     */
    public ScoringResult evaluate(Apprenant learner, Formation formation) {
        Objects.requireNonNull(formation, "Formation candidate cannot be null");

        if (learner == null) {
            // Unauthenticated / anonymous visitor fallback
            return new ScoringResult(50, 0.0, 0.0, 50.0, List.of(), List.of(),
                    List.of("Recommandation générale (profil non connecté)"));
        }

        // ── 1. Skills Score (0 - 100) ───────────────────────────────────────────
        Set<String> learnerSkills = FeatureExtractor.extractLearnerSkillTokens(learner);
        Set<String> formationTokens = FeatureExtractor.extractFormationTargetTokens(formation);

        List<String> matchedSkills = new ArrayList<>();
        double skillsScore = 0.0;

        if (!learnerSkills.isEmpty()) {
            for (String skill : learnerSkills) {
                if (formationTokens.contains(skill)) {
                    matchedSkills.add(capitalize(skill));
                }
            }
            skillsScore = Math.min(100.0, ((double) matchedSkills.size() / learnerSkills.size()) * 100.0);
        }

        // ── 2. Interests Score (0 - 100) ────────────────────────────────────────
        Set<String> learnerInterests = FeatureExtractor.extractLearnerInterestTokens(learner);
        Set<String> categoryTokens = FeatureExtractor.extractFormationCategoryTokens(formation);

        List<String> matchedInterests = new ArrayList<>();
        double interestsScore = 0.0;

        if (!learnerInterests.isEmpty()) {
            double accumulatedInterestPoints = 0.0;
            for (String interest : learnerInterests) {
                if (categoryTokens.contains(interest)) {
                    // Full match with category
                    accumulatedInterestPoints += 100.0;
                    matchedInterests.add(capitalize(interest));
                } else if (formationTokens.contains(interest)) {
                    // Match with formation tags, title or description
                    accumulatedInterestPoints += 75.0;
                    matchedInterests.add(capitalize(interest));
                }
            }
            interestsScore = Math.min(100.0, accumulatedInterestPoints / learnerInterests.size());
        }

        // ── 3. Level Compatibility Score (0 - 100) ──────────────────────────────
        Niveau learnerLevel = learner.getNiveau() != null ? learner.getNiveau() : Niveau.DEBUTANT;
        Niveau formationLevel = formation.getNiveau() != null ? formation.getNiveau() : Niveau.DEBUTANT;
        double levelScore = computeLevelScore(learnerLevel, formationLevel);

        // ── 4. Weighted Final Score (0 - 100) ───────────────────────────────────
        double rawScore = (WEIGHT_SKILLS * skillsScore)
                + (WEIGHT_INTERESTS * interestsScore)
                + (WEIGHT_LEVEL * levelScore);

        int finalScore = (int) Math.max(0, Math.min(100, Math.round(rawScore)));

        // ── 5. Generate Human-Readable Explanations ─────────────────────────────
        List<String> explications = generateExplanations(
                matchedSkills, skillsScore,
                matchedInterests, interestsScore,
                learnerLevel, formationLevel, levelScore
        );

        return new ScoringResult(
                finalScore,
                skillsScore,
                interestsScore,
                levelScore,
                matchedSkills,
                matchedInterests,
                explications
        );
    }

    /**
     * Pedagogical level compatibility matrix.
     */
    public double computeLevelScore(Niveau learnerLevel, Niveau formationLevel) {
        if (learnerLevel == null || formationLevel == null) {
            return 50.0;
        }

        return switch (learnerLevel) {
            case DEBUTANT -> switch (formationLevel) {
                case DEBUTANT -> 100.0;
                case INTERMEDIAIRE -> 70.0;
                case AVANCE -> 20.0;
            };
            case INTERMEDIAIRE -> switch (formationLevel) {
                case DEBUTANT -> 50.0;
                case INTERMEDIAIRE -> 100.0;
                case AVANCE -> 75.0;
            };
            case AVANCE -> switch (formationLevel) {
                case DEBUTANT -> 20.0;
                case INTERMEDIAIRE -> 60.0;
                case AVANCE -> 100.0;
            };
        };
    }

    /**
     * Builds contextual explanations detailing which exact attributes contributed to the score.
     */
    private List<String> generateExplanations(
            List<String> matchedSkills, double skillsScore,
            List<String> matchedInterests, double interestsScore,
            Niveau learnerLevel, Niveau formationLevel, double levelScore
    ) {
        List<String> reasons = new ArrayList<>();

        // Skill explanation
        if (!matchedSkills.isEmpty()) {
            int skillPoints = (int) Math.round(WEIGHT_SKILLS * skillsScore);
            reasons.add("Compétences correspondantes : " + String.join(", ", matchedSkills) + " (+" + skillPoints + "%)");
        }

        // Interest explanation
        if (!matchedInterests.isEmpty()) {
            int interestPoints = (int) Math.round(WEIGHT_INTERESTS * interestsScore);
            reasons.add("Intérêt aligné : " + String.join(", ", matchedInterests) + " (+" + interestPoints + "%)");
        }

        // Level explanation
        int levelPoints = (int) Math.round(WEIGHT_LEVEL * levelScore);
        if (learnerLevel == formationLevel) {
            reasons.add("Niveau " + formatNiveau(formationLevel) + " parfaitement adapté à votre profil (+" + levelPoints + "%)");
        } else if (levelScore >= 60.0) {
            reasons.add("Niveau " + formatNiveau(formationLevel) + " accessible pour votre niveau " + formatNiveau(learnerLevel) + " (+" + levelPoints + "%)");
        } else {
            reasons.add("Niveau " + formatNiveau(formationLevel) + " (écart de niveau avec votre profil : " + formatNiveau(learnerLevel) + ")");
        }

        if (reasons.isEmpty()) {
            reasons.add("Formation suggérée selon les thématiques générales de la plateforme");
        }

        return reasons;
    }

    private static String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    private static String formatNiveau(Niveau niveau) {
        if (niveau == null) return "";
        return switch (niveau) {
            case DEBUTANT -> "Débutant";
            case INTERMEDIAIRE -> "Intermédiaire";
            case AVANCE -> "Avancé";
        };
    }
}
