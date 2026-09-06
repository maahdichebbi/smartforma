package tn.esprit.smartforma.recommendation.engine;

import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.catalogue.entity.Formation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility for feature extraction and text tokenization for the MLA recommendation engine.
 *
 * Provides deterministic token normalization:
 * - Lowercasing and whitespace trimming
 * - Punctuation stripping while preserving tech terms (e.g., "c++", "c#", ".net")
 * - Stop-word elimination (French and English common grammatical noise)
 * - Composite phrase and individual token extraction
 */
public final class FeatureExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            // French stop words
            "le", "la", "les", "un", "une", "des", "du", "de", "d", "l",
            "et", "ou", "en", "dans", "par", "pour", "sur", "avec", "sans",
            "ce", "cet", "cette", "ces", "son", "sa", "ses", "leur", "leurs",
            "qui", "que", "quoi", "dont", "où", "au", "aux", "est", "sont",
            // English stop words
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for",
            "with", "without", "by", "from", "of", "about", "into", "through",
            "is", "are", "was", "were", "be", "been", "being"
    );

    // Splits text by whitespace and common punctuation (commas, semicolons, parentheses, slashes, brackets)
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("[\\s,;:!?()\\[\\]{}\"'\u201C\u201D/\\\\]+");

    private FeatureExtractor() {
        // utility class
    }

    /**
     * Tokenizes a raw string into a normalized set of distinct tokens.
     * Also includes normalized composite segments if delimited by comma/semicolon.
     *
     * Example: "Java, Spring Boot, SQL"
     * -> {"java", "spring", "boot", "spring boot", "sql"}
     */
    public static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> tokens = new HashSet<>();

        // 1. Extract comma/semicolon phrases first (e.g., "spring boot", "machine learning")
        String[] phrases = text.split("[,;]+");
        for (String phrase : phrases) {
            String cleanPhrase = phrase.trim().toLowerCase();
            if (cleanPhrase.length() >= 2 && !STOP_WORDS.contains(cleanPhrase)) {
                tokens.add(cleanPhrase);
            }
        }

        // 2. Tokenize into individual words
        String[] words = DELIMITER_PATTERN.split(text.toLowerCase());
        for (String word : words) {
            String clean = word.replaceAll("^[._-]+|[._-]+$", "").trim();
            if (clean.length() >= 2 && !STOP_WORDS.contains(clean)) {
                tokens.add(clean);
            }
        }

        return Collections.unmodifiableSet(tokens);
    }

    /**
     * Extracts normalized skill tokens from a learner profile.
     */
    public static Set<String> extractLearnerSkillTokens(Apprenant learner) {
        if (learner == null || learner.getCompetences() == null) {
            return Collections.emptySet();
        }
        return tokenize(learner.getCompetences());
    }

    /**
     * Extracts normalized interest tokens from a learner profile.
     */
    public static Set<String> extractLearnerInterestTokens(Apprenant learner) {
        if (learner == null || learner.getInterets() == null) {
            return Collections.emptySet();
        }
        return tokenize(learner.getInterets());
    }

    /**
     * Extracts all target feature tokens for a formation:
     * - tags / mots-clés (highest priority)
     * - formation title
     * - category name
     * - chapter titles
     * - formation description
     */
    public static Set<String> extractFormationTargetTokens(Formation formation) {
        if (formation == null) {
            return Collections.emptySet();
        }

        Set<String> allTokens = new HashSet<>();

        // 1. Tags
        if (formation.getTags() != null) {
            allTokens.addAll(tokenize(formation.getTags()));
        }

        // 2. Title
        if (formation.getTitre() != null) {
            allTokens.addAll(tokenize(formation.getTitre()));
        }

        // 3. Category
        if (formation.getCategorie() != null && formation.getCategorie().getNom() != null) {
            allTokens.addAll(tokenize(formation.getCategorie().getNom()));
        }

        // 4. Chapters
        if (formation.getChapitres() != null) {
            for (var ch : formation.getChapitres()) {
                if (ch != null && ch.getTitre() != null) {
                    allTokens.addAll(tokenize(ch.getTitre()));
                }
            }
        }

        // 5. Description keywords
        if (formation.getDescription() != null) {
            allTokens.addAll(tokenize(formation.getDescription()));
        }

        return Collections.unmodifiableSet(allTokens);
    }

    /**
     * Extracts category and domain-specific tokens for a formation.
     */
    public static Set<String> extractFormationCategoryTokens(Formation formation) {
        if (formation == null || formation.getCategorie() == null || formation.getCategorie().getNom() == null) {
            return Collections.emptySet();
        }
        return tokenize(formation.getCategorie().getNom());
    }
}
