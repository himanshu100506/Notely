package com.notely.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Note} — constructors, getters/setters,
 * tag management (HashSet behaviour), and defensive copying.
 */
class NoteTest {

    // ───────────────── Constructor tests ─────────────────

    @Test
    void noArgConstructorCreatesEmptyNote() {
        Note note = new Note();
        assertEquals(0, note.getId());
        assertNull(note.getTitle());
        assertNull(note.getContent());
        assertFalse(note.isPinned());
        assertNull(note.getFolderId(), "folderId should be null before assignment");
        assertTrue(note.getTags().isEmpty());
    }

    @Test
    void convenienceConstructorSetsDefaults() {
        Note note = new Note("Ideas", "Some thoughts");

        assertEquals("Ideas", note.getTitle());
        assertEquals("Some thoughts", note.getContent());
        assertFalse(note.isPinned(), "New notes start un-pinned");
        assertNotNull(note.getCreatedAt());
        assertNotNull(note.getUpdatedAt());
    }

    @Test
    void fullConstructorSetsAllFields() {
        LocalDateTime created = LocalDateTime.of(2026, 3, 1, 9, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 3, 2, 14, 30);

        Note note = new Note(42, "Report", "Q1 results", true, 5, created, updated);

        assertEquals(42, note.getId());
        assertEquals("Report", note.getTitle());
        assertEquals("Q1 results", note.getContent());
        assertTrue(note.isPinned());
        assertEquals(5, note.getFolderId());
        assertEquals(created, note.getCreatedAt());
        assertEquals(updated, note.getUpdatedAt());
    }

    // ───────────────── Getter / Setter tests ─────────────

    @Test
    void settersUpdateAllFields() {
        Note note = new Note();
        LocalDateTime now = LocalDateTime.now();

        note.setId(1);
        note.setTitle("Updated");
        note.setContent("New content");
        note.setPinned(true);
        note.setFolderId(3);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        assertEquals(1, note.getId());
        assertEquals("Updated", note.getTitle());
        assertEquals("New content", note.getContent());
        assertTrue(note.isPinned());
        assertEquals(3, note.getFolderId());
        assertEquals(now, note.getCreatedAt());
        assertEquals(now, note.getUpdatedAt());
    }

    @Test
    void folderIdCanBeSetToNull() {
        Note note = new Note("Test", "content");
        note.setFolderId(5);
        assertEquals(5, note.getFolderId());

        note.setFolderId(null);
        assertNull(note.getFolderId(), "null folderId means no folder (SQL NULL)");
    }

    // ──────────────── Tag management (HashSet) ───────────

    @Test
    void addTagIncreasesSetSize() {
        Note note = new Note("Test", "content");
        note.addTag(new Tag(1, "java"));

        assertEquals(1, note.getTags().size());
    }

    @Test
    void addNullTagIsIgnored() {
        Note note = new Note("Test", "content");
        note.addTag(null);
        assertTrue(note.getTags().isEmpty());
    }

    @Test
    void addDuplicateTagByIdIsIgnored() {
        Note note = new Note("Test", "content");
        note.addTag(new Tag(1, "java"));
        note.addTag(new Tag(1, "java"));    // same id

        assertEquals(1, note.getTags().size(),
                "HashSet must prevent duplicate tags with the same id");
    }

    @Test
    void removeTagDecreasesSetSize() {
        Note note = new Note("Test", "content");
        Tag tag = new Tag(1, "java");
        note.addTag(tag);

        note.removeTag(tag);

        assertTrue(note.getTags().isEmpty());
    }

    @Test
    void removeTagByEqualObject() {
        Note note = new Note("Test", "content");
        note.addTag(new Tag(1, "java"));

        // Remove using a different object with the same id
        note.removeTag(new Tag(1, "java"));

        assertTrue(note.getTags().isEmpty(),
                "removeTag should work by equals(), not reference");
    }

    @Test
    void hasTagReturnsTrueWhenPresent() {
        Note note = new Note("Test", "content");
        note.addTag(new Tag(1, "java"));

        assertTrue(note.hasTag(new Tag(1, "java")));
    }

    @Test
    void hasTagReturnsFalseWhenAbsent() {
        Note note = new Note("Test", "content");
        assertFalse(note.hasTag(new Tag(99, "missing")));
    }

    @Test
    void getTagsReturnsUnmodifiableSet() {
        Note note = new Note("Test", "content");
        note.addTag(new Tag(1, "java"));

        Set<Tag> tags = note.getTags();

        assertThrows(UnsupportedOperationException.class,
                () -> tags.add(new Tag(2, "hacked")),
                "Returned set must be unmodifiable");
    }

    // ───────────────── toString ──────────────────────────

    @Test
    void toStringContainsTitleAndTagCount() {
        Note note = new Note("Shopping list", "milk, eggs");
        note.addTag(new Tag(1, "errands"));
        note.addTag(new Tag(2, "home"));

        String result = note.toString();
        assertTrue(result.contains("Shopping list"));
        assertTrue(result.contains("tags=2"));
    }
}
