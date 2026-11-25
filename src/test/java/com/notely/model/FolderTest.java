package com.notely.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Folder} — constructors, getters/setters,
 * composition (note list management), and defensive copying.
 */
class FolderTest {

    // ───────────────── Constructor tests ─────────────────

    @Test
    void noArgConstructorCreatesEmptyFolder() {
        Folder folder = new Folder();
        assertEquals(0, folder.getId());
        assertNull(folder.getName());
        assertNull(folder.getCreatedAt());
        assertEquals(0, folder.getNoteCount());
    }

    @Test
    void nameConstructorSetsNameAndTimestamp() {
        Folder folder = new Folder("Work");
        assertEquals("Work", folder.getName());
        assertNotNull(folder.getCreatedAt(), "createdAt should be set automatically");
    }

    @Test
    void fullConstructorSetsAllFields() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 15, 10, 30);
        Folder folder = new Folder(1, "Personal", ts);

        assertEquals(1, folder.getId());
        assertEquals("Personal", folder.getName());
        assertEquals(ts, folder.getCreatedAt());
    }

    // ───────────────── Getter / Setter tests ─────────────

    @Test
    void settersUpdateFields() {
        Folder folder = new Folder();
        LocalDateTime ts = LocalDateTime.now();

        folder.setId(7);
        folder.setName("Recipes");
        folder.setCreatedAt(ts);

        assertEquals(7, folder.getId());
        assertEquals("Recipes", folder.getName());
        assertEquals(ts, folder.getCreatedAt());
    }

    // ───────────────── Composition — note management ─────

    @Test
    void addNoteIncreasesCount() {
        Folder folder = new Folder("Work");
        Note note = new Note("Meeting", "Q4 planning");

        folder.addNote(note);

        assertEquals(1, folder.getNoteCount());
    }

    @Test
    void addNullNoteIsIgnored() {
        Folder folder = new Folder("Work");
        folder.addNote(null);
        assertEquals(0, folder.getNoteCount());
    }

    @Test
    void addSameNoteReferenceIsIgnored() {
        Folder folder = new Folder("Work");
        Note note = new Note("Meeting", "Q4 planning");

        folder.addNote(note);
        folder.addNote(note);     // duplicate reference

        assertEquals(1, folder.getNoteCount(),
                "Same note reference should not be added twice");
    }

    @Test
    void removeNoteDecreasesCount() {
        Folder folder = new Folder("Work");
        Note note = new Note("Meeting", "Q4 planning");
        folder.addNote(note);

        folder.removeNote(note);

        assertEquals(0, folder.getNoteCount());
    }

    @Test
    void getNotesReturnsUnmodifiableList() {
        Folder folder = new Folder("Work");
        folder.addNote(new Note("A", "content"));

        List<Note> notes = folder.getNotes();

        assertThrows(UnsupportedOperationException.class,
                () -> notes.add(new Note("B", "hacked in")),
                "Returned list must be unmodifiable");
    }

    @Test
    void getNotesReflectsInternalState() {
        Folder folder = new Folder("Work");
        Note note = new Note("A", "content");
        folder.addNote(note);

        List<Note> notes = folder.getNotes();
        assertEquals(1, notes.size());
        assertSame(note, notes.get(0));
    }

    // ───────────────── toString ──────────────────────────

    @Test
    void toStringContainsNameAndNoteCount() {
        Folder folder = new Folder(1, "Work", LocalDateTime.now());
        folder.addNote(new Note("A", "a"));
        folder.addNote(new Note("B", "b"));

        String result = folder.toString();
        assertTrue(result.contains("Work"));
        assertTrue(result.contains("notes=2"));
    }
}
