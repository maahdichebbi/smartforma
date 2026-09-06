package tn.esprit.smartforma.recommendation.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationEngineTest {

    private RecommendationEngine engine;
    private Categorie backendCategory;
    private Categorie managementCategory;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();

        backendCategory = new Categorie();
        backendCategory.setId(1L);
        backendCategory.setNom("Développement Backend");

        managementCategory = new Categorie();
        managementCategory.setId(2L);
        managementCategory.setNom("Management & Agile");
    }

    private Formation createFormation(String title, String tags, Niveau niveau, Categorie category) {
        Formation f = new Formation();
        f.setId(10L);
        f.setTitre(title);
        f.setDescription("Comprehensive practical course on " + title);
        f.setPrix(new BigDecimal("200.00"));
        f.setDureeHeures(20);
        f.setNiveau(niveau);
        f.setCategorie(category);
        f.setTags(tags);
        f.setChapitres(new ArrayList<>());
        return f;
    }

    private Apprenant createLearner(String skills, String interests, Niveau niveau) {
        Apprenant a = new Apprenant();
        a.setId(1L);
        a.setNom("Doe");
        a.setPrenom("John");
        a.setEmail("john.doe@example.com");
        a.setCompetences(skills);
        a.setInterets(interests);
        a.setNiveau(niveau);
        return a;
    }

    @Test
    @DisplayName("1. Perfect skill match — all learner skills present in formation tags")
    void testPerfectSkillMatch() {
        Apprenant learner = createLearner("Java, Spring Boot", "Backend", Niveau.INTERMEDIAIRE);
        Formation formation = createFormation("Spring Boot Masterclass", "java, spring boot, rest api", Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        assertEquals(100.0, result.skillsScore());
        assertTrue(result.finalScore() >= 90);
        assertTrue(result.matchedSkills().contains("Java"));
    }

    @Test
    @DisplayName("2. Partial skill match — some skills match, some do not")
    void testPartialSkillMatch() {
        Apprenant learner = createLearner("Java, Python, Docker", "Cloud", Niveau.INTERMEDIAIRE);
        Formation formation = createFormation("Java Architecture", "java, design patterns", Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        // 1 out of 3 skills matched = 33.3%
        assertEquals(33.3, result.skillsScore(), 1.0);
        assertEquals(1, result.matchedSkills().size());
        assertTrue(result.matchedSkills().contains("Java"));
    }

    @Test
    @DisplayName("3. No skill match — zero overlap between learner and formation")
    void testNoSkillMatch() {
        Apprenant learner = createLearner("PHP, Laravel", "Web", Niveau.DEBUTANT);
        Formation formation = createFormation("Scrum Master Certification", "agile, scrum, kanban", Niveau.DEBUTANT, managementCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        assertEquals(0.0, result.skillsScore());
        assertTrue(result.matchedSkills().isEmpty());
    }

    @Test
    @DisplayName("4. Interest match — interest matches formation tags and description")
    void testInterestMatch() {
        Apprenant learner = createLearner("Java", "Microservices, Cloud", Niveau.INTERMEDIAIRE);
        Formation formation = createFormation("Cloud Microservices", "docker, kubernetes, microservices, cloud", Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        assertTrue(result.interestsScore() >= 75.0);
        assertFalse(result.matchedInterests().isEmpty());
    }

    @Test
    @DisplayName("5. Category match — interest matches formation category name exactly")
    void testCategoryMatch() {
        Apprenant learner = createLearner("Git", "Management", Niveau.DEBUTANT);
        Formation formation = createFormation("Agile Leadership", "leadership, scrum", Niveau.DEBUTANT, managementCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        // Matches category name "Management & Agile"
        assertEquals(100.0, result.interestsScore());
        assertTrue(result.matchedInterests().contains("Management"));
    }

    @Test
    @DisplayName("6. Level compatibility — checks matrix transitions and penalties")
    void testLevelCompatibility() {
        // Beginner learner
        assertEquals(100.0, engine.computeLevelScore(Niveau.DEBUTANT, Niveau.DEBUTANT));
        assertEquals(70.0, engine.computeLevelScore(Niveau.DEBUTANT, Niveau.INTERMEDIAIRE));
        assertEquals(20.0, engine.computeLevelScore(Niveau.DEBUTANT, Niveau.AVANCE));

        // Intermediate learner
        assertEquals(50.0, engine.computeLevelScore(Niveau.INTERMEDIAIRE, Niveau.DEBUTANT));
        assertEquals(100.0, engine.computeLevelScore(Niveau.INTERMEDIAIRE, Niveau.INTERMEDIAIRE));
        assertEquals(75.0, engine.computeLevelScore(Niveau.INTERMEDIAIRE, Niveau.AVANCE));

        // Advanced learner
        assertEquals(20.0, engine.computeLevelScore(Niveau.AVANCE, Niveau.DEBUTANT));
        assertEquals(60.0, engine.computeLevelScore(Niveau.AVANCE, Niveau.INTERMEDIAIRE));
        assertEquals(100.0, engine.computeLevelScore(Niveau.AVANCE, Niveau.AVANCE));
    }

    @Test
    @DisplayName("7. Empty learner profile — skills and interests null or empty")
    void testEmptyLearnerProfile() {
        Apprenant learner = createLearner(null, "", Niveau.DEBUTANT);
        Formation formation = createFormation("Spring Boot", "java, spring", Niveau.DEBUTANT, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        assertEquals(0.0, result.skillsScore());
        assertEquals(0.0, result.interestsScore());
        assertEquals(100.0, result.levelScore());
        // Level score weighted 25% = 25 final score
        assertEquals(25, result.finalScore());
        assertFalse(result.explications().isEmpty());
    }

    @Test
    @DisplayName("8. Empty formation tags — engine falls back to title, category and description tokens")
    void testEmptyFormationTags() {
        Apprenant learner = createLearner("Java, Spring", "Backend", Niveau.INTERMEDIAIRE);
        // formation tags are null
        Formation formation = createFormation("Spring Masterclass", null, Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        // "spring" is in the title, and "backend" is in the category name
        assertTrue(result.skillsScore() > 0.0);
        assertTrue(result.interestsScore() > 0.0);
    }

    @Test
    @DisplayName("11. Final score remains strictly clamped between 0 and 100")
    void testScoreBounds() {
        Apprenant learner1 = createLearner("Java, Spring, SQL, Docker", "Backend, Cloud", Niveau.INTERMEDIAIRE);
        Formation formation1 = createFormation("Spring Boot & Cloud", "java, spring, sql, docker, backend, cloud", Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult maxResult = engine.evaluate(learner1, formation1);
        assertTrue(maxResult.finalScore() <= 100);
        assertTrue(maxResult.finalScore() >= 0);

        Apprenant learner2 = createLearner("Rust", "Embedded", Niveau.DEBUTANT);
        Formation formation2 = createFormation("Agile Executive", "finance, hr", Niveau.AVANCE, managementCategory);

        ScoringResult minResult = engine.evaluate(learner2, formation2);
        assertTrue(minResult.finalScore() <= 100);
        assertTrue(minResult.finalScore() >= 0);
    }

    @Test
    @DisplayName("12. Explanations correspond to actual matching factors and percentage weights")
    void testExplanationsReflectMatchingFactors() {
        Apprenant learner = createLearner("Java", "Backend", Niveau.INTERMEDIAIRE);
        Formation formation = createFormation("Java Enterprise", "java, backend", Niveau.INTERMEDIAIRE, backendCategory);

        ScoringResult result = engine.evaluate(learner, formation);

        assertNotNull(result.explications());
        assertTrue(result.explications().stream().anyMatch(e -> e.contains("Compétences correspondantes") && e.contains("Java")));
        assertTrue(result.explications().stream().anyMatch(e -> e.contains("Intérêt aligné") && e.contains("Backend")));
        assertTrue(result.explications().stream().anyMatch(e -> e.contains("Niveau Intermédiaire")));
    }
}
