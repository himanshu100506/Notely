package com.notely.util;

import com.notely.model.Note;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts keywords from text using word-frequency analysis.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Tokenise — split on non-letter boundaries, lowercase.</li>
 *   <li>Filter — drop stop words and tokens shorter than 3 characters.</li>
 *   <li>Count — accumulate frequencies in a {@link HashMap}.</li>
 *   <li>Rank — sort by frequency descending, then alphabetically
 *       for deterministic tie-breaking.</li>
 * </ol>
 *
 * <h3>Design</h3>
 * This class is a pure utility with no database access.
 * It receives data (strings or lists of Notes) and returns results.
 */
public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "all", "also", "am",
            "an", "and", "any", "are", "as", "at", "be", "because", "been",
            "before", "being", "between", "both", "but", "by", "can",
            "could", "did", "do", "does", "down", "each", "even", "few",
            "for", "from", "get", "got", "had", "has", "have", "he", "her",
            "here", "him", "his", "how", "if", "in", "into", "is", "it",
            "its", "just", "may", "me", "might", "more", "most", "must",
            "my", "new", "no", "nor", "not", "now", "of", "on", "only",
            "or", "other", "our", "out", "over", "own", "same", "she",
            "should", "so", "some", "still", "such", "than", "that", "the",
            "their", "them", "then", "there", "these", "they", "this",
            "those", "through", "to", "too", "under", "until", "up", "us",
            "very", "was", "we", "were", "what", "when", "where", "which",
            "while", "who", "why", "will", "with", "would", "you", "your"
    );

    private static final int MIN_WORD_LENGTH = 3;

    /**
     * Extracts the top keywords from a block of text.
     *
     * @param content     the text to analyse
     * @param maxKeywords maximum number of keywords to return
     * @return ranked list of keywords (most frequent first);
     *         ties are broken alphabetically for determinism
     */
    public List<String> extractKeywords(String content, int maxKeywords) {
        if (content == null || content.isBlank() || maxKeywords <= 0) {
            return List.of();
        }

        List<String> tokens = tokenize(content);
        HashMap<String, Integer> frequency = new HashMap<>();
        for (String token : tokens) {
            frequency.merge(token, 1, Integer::sum);
        }

        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(maxKeywords)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Suggests keywords related to a given keyword by analysing
     * co-occurrence across a set of notes.
     *
     * <p>Only notes whose title or content contain the keyword are
     * considered.  The original keyword and stop words are excluded
     * from the results.</p>
     *
     * @param notes          the notes to analyse
     * @param keyword        the reference keyword
     * @param maxSuggestions maximum number of suggestions to return
     * @return related keywords ranked by co-occurrence count;
     *         ties are broken alphabetically for determinism
     */
    public List<String> suggestRelated(List<Note> notes, String keyword,
                                       int maxSuggestions) {
        if (notes == null || keyword == null || keyword.isBlank()
                || maxSuggestions <= 0) {
            return List.of();
        }

        String normalizedKeyword = keyword.toLowerCase().trim();

        HashMap<String, Integer> cooccurrence = new HashMap<>();

        for (Note note : notes) {
            String text = combine(note.getTitle(), note.getContent());
            List<String> tokens = tokenize(text);

            if (!tokens.contains(normalizedKeyword)) {
                continue;
            }

            Set<String> unique = new HashSet<>(tokens);
            unique.remove(normalizedKeyword);

            for (String word : unique) {
                cooccurrence.merge(word, 1, Integer::sum);
            }
        }

        return cooccurrence.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(maxSuggestions)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ───────────────── Internals ─────────────────────────

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String raw : text.toLowerCase().split("[^a-zA-Z]+")) {
            if (raw.length() >= MIN_WORD_LENGTH && !STOP_WORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private String combine(String title, String content) {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title);
        sb.append(' ');
        if (content != null) sb.append(content);
        return sb.toString();
    }
}
