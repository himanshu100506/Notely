package com.notely.util;

import com.notely.model.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KeywordExtractor}.
 *
 * Pure in-memory tests — no database connection required.
 */
class KeywordExtractorTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    // ─────────────── extractKeywords ─────────────────────

    @Test
    void extractKeywordsRanksbyFrequency() {
        String content = "java java java python python rust";
        List<String> keywords = extractor.extractKeywords(content, 3);

        assertEquals(3, keywords.size());
        assertEquals("java", keywords.get(0));
        assertEquals("python", keywords.get(1));
        assertEquals("rust", keywords.get(2));
    }

    @Test
    void extractKeywordsBreaksTiesAlphabetically() {
        String content = "delta alpha charlie bravo";
        List<String> keywords = extractor.extractKeywords(content, 4);

        assertEquals(List.of("alpha", "bravo", "charlie", "delta"), keywords);
    }

    @Test
    void extractKeywordsRespectsMaxLimit() {
        String content = "apple banana cherry date elderberry";
        List<String> keywords = extractor.extractKeywords(content, 2);

        assertEquals(2, keywords.size());
    }

    @Test
    void extractKeywordsExcludesStopWords() {
        String content = "the quick brown fox jumps over the lazy dog";
        List<String> keywords = extractor.extractKeywords(content, 10);

        assertFalse(keywords.contains("the"));
        assertFalse(keywords.contains("over"));
        assertTrue(keywords.contains("quick"));
        assertTrue(keywords.contains("brown"));
        assertTrue(keywords.contains("fox"));
    }

    @Test
    void extractKeywordsExcludesShortWords() {
        String content = "go to be or no an if it is do";
        List<String> keywords = extractor.extractKeywords(content, 10);

        assertTrue(keywords.isEmpty(),
                "All tokens are either stop words or shorter than 3 chars");
    }

    @Test
    void extractKeywordsWithNullReturnsEmpty() {
        assertEquals(List.of(), extractor.extractKeywords(null, 5));
    }

    @Test
    void extractKeywordsWithEmptyReturnsEmpty() {
        assertEquals(List.of(), extractor.extractKeywords("", 5));
    }

    @Test
    void extractKeywordsWithBlankReturnsEmpty() {
        assertEquals(List.of(), extractor.extractKeywords("   ", 5));
    }

    @Test
    void extractKeywordsWithZeroMaxReturnsEmpty() {
        assertEquals(List.of(), extractor.extractKeywords("hello world test", 0));
    }

    @Test
    void extractKeywordsWithNegativeMaxReturnsEmpty() {
        assertEquals(List.of(), extractor.extractKeywords("hello world test", -1));
    }

    @Test
    void extractKeywordsIgnoresPunctuationAndCase() {
        String content = "Java, JAVA! java. Python: python";
        List<String> keywords = extractor.extractKeywords(content, 2);

        assertEquals("java", keywords.get(0));
        assertEquals("python", keywords.get(1));
    }

    @Test
    void extractKeywordsHandlesNumbersInText() {
        String content = "version 3 release 2024 java framework spring";
        List<String> keywords = extractor.extractKeywords(content, 10);

        assertTrue(keywords.contains("java"));
        assertTrue(keywords.contains("version"));
        assertFalse(keywords.stream().anyMatch(k -> k.matches("\\d+")),
                "Numeric-only tokens should not appear");
    }

    // ─────────────── suggestRelated ──────────────────────

    @Test
    void suggestRelatedFindsCooccurringWords() {
        List<Note> notes = List.of(
                new Note("Java Tutorial", "Learn java programming basics"),
                new Note("Java Advanced", "Advanced java concurrency patterns"),
                new Note("Python Guide", "Learn python scripting")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 5);

        assertTrue(related.contains("learn"),
                "learn co-occurs with java in note 1");
        assertTrue(related.contains("advanced"),
                "advanced co-occurs with java in note 2");
    }

    @Test
    void suggestRelatedExcludesTheSearchKeyword() {
        List<Note> notes = List.of(
                new Note("Java", "java java java programming")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 10);

        assertFalse(related.contains("java"),
                "The search keyword itself must be excluded");
    }

    @Test
    void suggestRelatedExcludesStopWords() {
        List<Note> notes = List.of(
                new Note("Java", "the best java framework is spring")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 10);

        assertFalse(related.contains("the"));
        assertFalse(related.contains("is"));
    }

    @Test
    void suggestRelatedIgnoresNotesWithoutKeyword() {
        List<Note> notes = List.of(
                new Note("Java Intro", "learn java basics"),
                new Note("Cooking", "learn delicious recipes pasta")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 10);

        assertFalse(related.contains("recipes"),
                "Words from notes without the keyword should not appear");
        assertFalse(related.contains("pasta"));
        assertFalse(related.contains("delicious"));
    }

    @Test
    void suggestRelatedRanksbyCooccurrenceCount() {
        List<Note> notes = List.of(
                new Note("Java Web", "java spring framework"),
                new Note("Java API", "java spring rest endpoints"),
                new Note("Java CLI", "java command line tools")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 2);

        assertEquals("spring", related.get(0),
                "spring co-occurs in 2 notes, more than any other");
    }

    @Test
    void suggestRelatedBreaksTiesAlphabetically() {
        List<Note> notes = List.of(
                new Note("An Op", "java zebra alpha mango")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 10);

        assertEquals("alpha", related.get(0));
        assertEquals("mango", related.get(1));
        assertEquals("zebra", related.get(2));
    }

    @Test
    void suggestRelatedWithNullNotesReturnsEmpty() {
        assertEquals(List.of(), extractor.suggestRelated(null, "java", 5));
    }

    @Test
    void suggestRelatedWithNullKeywordReturnsEmpty() {
        assertEquals(List.of(),
                extractor.suggestRelated(List.of(), null, 5));
    }

    @Test
    void suggestRelatedWithBlankKeywordReturnsEmpty() {
        assertEquals(List.of(),
                extractor.suggestRelated(List.of(), "  ", 5));
    }

    @Test
    void suggestRelatedWithZeroMaxReturnsEmpty() {
        List<Note> notes = List.of(new Note("Java", "java test"));
        assertEquals(List.of(),
                extractor.suggestRelated(notes, "java", 0));
    }

    @Test
    void suggestRelatedWithNoMatchingNotesReturnsEmpty() {
        List<Note> notes = List.of(
                new Note("Python", "python scripting guide")
        );
        List<String> related = extractor.suggestRelated(notes, "java", 5);
        assertTrue(related.isEmpty());
    }

    @Test
    void suggestRelatedIsCaseInsensitive() {
        List<Note> notes = List.of(
                new Note("JAVA Tutorial", "Learn JAVA Programming")
        );

        List<String> related = extractor.suggestRelated(notes, "Java", 10);

        assertFalse(related.isEmpty(),
                "Should match regardless of case");
        assertTrue(related.contains("learn"));
    }

    @Test
    void suggestRelatedRespectsMaxLimit() {
        List<Note> notes = List.of(
                new Note("Java", "java alpha bravo charlie delta echo")
        );

        List<String> related = extractor.suggestRelated(notes, "java", 2);
        assertEquals(2, related.size());
    }
}
