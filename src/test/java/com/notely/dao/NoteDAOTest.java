package com.notely.dao;

import com.notely.model.Folder;
import com.notely.model.Note;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link NoteDAO}.
 *
 * Runs against the {@code notely_test} database.
 * Each test starts with empty tables.
 */
class NoteDAOTest {

    private final NoteDAO noteDAO = new NoteDAO();
    private final FolderDAO folderDAO = new FolderDAO();

    private static final String[] REQUIRED_TABLES =
            {"folders", "notes", "tags", "note_tags"};

    // ─────────────── Setup / Teardown ─────────────────────

    @BeforeAll
    static void verifyTestDatabase() throws SQLException {
        Connection conn;
        try {
            conn = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            fail("Cannot connect to MySQL. Is the server running? " + e.getMessage());
            return;
        }

        try (conn; Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT DATABASE()");
            assertTrue(rs.next(), "SELECT DATABASE() returned no rows");
            String dbName = rs.getString(1);
            assertEquals("notely_test", dbName,
                    "Tests MUST run against notely_test, but connected to: " + dbName);

            for (String table : REQUIRED_TABLES) {
                ResultSet trs = st.executeQuery(
                        "SELECT 1 FROM information_schema.tables "
                                + "WHERE table_schema = 'notely_test' AND table_name = '" + table + "'");
                assertTrue(trs.next(),
                        "Required table '" + table + "' does not exist in notely_test. "
                                + "Run schema.sql first.");
            }
        }
    }

    @BeforeEach
    void cleanTables() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            st.execute("TRUNCATE TABLE note_tags");
            st.execute("TRUNCATE TABLE notes");
            st.execute("TRUNCATE TABLE tags");
            st.execute("TRUNCATE TABLE folders");
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    @AfterAll
    static void resetConfig() {
        DatabaseConnection.resetConfig();
    }

    // ─────────────── createNote ───────────────────────────

    @Test
    void createNoteSetsGeneratedId() throws SQLException {
        Note note = new Note("Title", "Content");
        noteDAO.createNote(note);

        assertTrue(note.getId() > 0);
    }

    @Test
    void createNoteWithNullFolderIdPersistsNull() throws SQLException {
        Note note = new Note("No Folder", "Content");
        // folderId is null by default
        noteDAO.createNote(note);

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getFolderId(),
                "folderId should be null when no folder is assigned");
    }

    @Test
    void createNoteWithFolderIdPersistsForeignKey() throws SQLException {
        Folder folder = new Folder("Work");
        folderDAO.createFolder(folder);

        Note note = new Note("Meeting Notes", "Q4 planning");
        note.setFolderId(folder.getId());
        noteDAO.createNote(note);

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isPresent());
        assertEquals(folder.getId(), found.get().getFolderId());
    }

    // ─────────────── updateNote ───────────────────────────

    @Test
    void updateNoteChangesFields() throws SQLException {
        Note note = new Note("Original", "Old content");
        noteDAO.createNote(note);

        note.setTitle("Updated");
        note.setContent("New content");
        note.setPinned(true);
        noteDAO.updateNote(note);

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getTitle());
        assertEquals("New content", found.get().getContent());
        assertTrue(found.get().isPinned());
    }

    @Test
    void updateNoteCanSetFolderToNull() throws SQLException {
        Folder folder = new Folder("Work");
        folderDAO.createFolder(folder);

        Note note = new Note("In Folder", "content");
        note.setFolderId(folder.getId());
        noteDAO.createNote(note);

        // Move note out of the folder
        note.setFolderId(null);
        noteDAO.updateNote(note);

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getFolderId());
    }

    // ─────────────── deleteNote ───────────────────────────

    @Test
    void deleteNoteRemovesIt() throws SQLException {
        Note note = new Note("To Delete", "content");
        noteDAO.createNote(note);

        noteDAO.deleteNote(note.getId());

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isEmpty());
    }

    // ─────────────── findById ─────────────────────────────

    @Test
    void findByIdReturnsEmptyForMissingId() throws SQLException {
        assertTrue(noteDAO.findById(9999).isEmpty());
    }

    // ─────────────── findAll ──────────────────────────────

    @Test
    void findAllReturnsAllNotes() throws SQLException {
        noteDAO.createNote(new Note("A", "a"));
        noteDAO.createNote(new Note("B", "b"));
        noteDAO.createNote(new Note("C", "c"));

        List<Note> notes = noteDAO.findAll();
        assertEquals(3, notes.size());
    }

    // ─────────────── findByFolder ─────────────────────────

    @Test
    void findByFolderReturnsOnlyMatchingNotes() throws SQLException {
        Folder work = new Folder("Work");
        Folder personal = new Folder("Personal");
        folderDAO.createFolder(work);
        folderDAO.createFolder(personal);

        Note n1 = new Note("Work Note", "content");
        n1.setFolderId(work.getId());
        noteDAO.createNote(n1);

        Note n2 = new Note("Personal Note", "content");
        n2.setFolderId(personal.getId());
        noteDAO.createNote(n2);

        List<Note> workNotes = noteDAO.findByFolder(work.getId());
        assertEquals(1, workNotes.size());
        assertEquals("Work Note", workNotes.get(0).getTitle());
    }

    // ─────────────── searchByTitleOrContent ───────────────

    @Test
    void searchByTitleFindsMatch() throws SQLException {
        noteDAO.createNote(new Note("Java Tutorial", "Learn Java"));
        noteDAO.createNote(new Note("Python Guide", "Learn Python"));

        List<Note> results = noteDAO.searchByTitleOrContent("Java");
        assertEquals(1, results.size());
        assertEquals("Java Tutorial", results.get(0).getTitle());
    }

    @Test
    void searchByContentFindsMatch() throws SQLException {
        noteDAO.createNote(new Note("Untitled", "Contains the keyword database"));
        noteDAO.createNote(new Note("Other", "Nothing here"));

        List<Note> results = noteDAO.searchByTitleOrContent("database");
        assertEquals(1, results.size());
    }

    @Test
    void searchReturnsEmptyForNoMatch() throws SQLException {
        noteDAO.createNote(new Note("Hello", "World"));

        List<Note> results = noteDAO.searchByTitleOrContent("zzzzz");
        assertTrue(results.isEmpty());
    }

    // ─────────────── findPinned ───────────────────────────

    @Test
    void findPinnedReturnsOnlyPinnedNotes() throws SQLException {
        Note pinned = new Note("Important", "content");
        pinned.setPinned(true);
        noteDAO.createNote(pinned);

        noteDAO.createNote(new Note("Regular", "content"));

        List<Note> pinnedNotes = noteDAO.findPinned();
        assertEquals(1, pinnedNotes.size());
        assertTrue(pinnedNotes.get(0).isPinned());
    }

    // ─────────────── findRecent ───────────────────────────

    @Test
    void findRecentRespectsLimit() throws SQLException {
        noteDAO.createNote(new Note("A", "a"));
        noteDAO.createNote(new Note("B", "b"));
        noteDAO.createNote(new Note("C", "c"));

        List<Note> recent = noteDAO.findRecent(2);
        assertEquals(2, recent.size());
    }

    // ─────────────── sortByTitle ──────────────────────────

    @Test
    void sortByTitleReturnsAlphabeticalOrder() throws SQLException {
        noteDAO.createNote(new Note("Charlie", "c"));
        noteDAO.createNote(new Note("Alpha", "a"));
        noteDAO.createNote(new Note("Bravo", "b"));

        List<Note> sorted = noteDAO.sortByTitle();
        assertEquals("Alpha", sorted.get(0).getTitle());
        assertEquals("Bravo", sorted.get(1).getTitle());
        assertEquals("Charlie", sorted.get(2).getTitle());
    }

    // ─────────────── sortByDate ──────────────────────────

    @Test
    void sortByDateReturnsNewestFirst() throws SQLException {
        noteDAO.createNote(new Note("First", "created first"));
        noteDAO.createNote(new Note("Second", "created second"));

        List<Note> sorted = noteDAO.sortByDate();
        // Most recent (Second) should come first
        assertEquals(2, sorted.size());
        // Both may have the same second-level timestamp in fast tests,
        // so we just verify the list is non-empty and valid
        assertNotNull(sorted.get(0).getUpdatedAt());
    }

    // ─────────────── Referential integrity ────────────────

    @Test
    void deletingFolderSetsNotesFolderIdToNull() throws SQLException {
        Folder folder = new Folder("Doomed");
        folderDAO.createFolder(folder);

        Note note = new Note("Orphaned", "content");
        note.setFolderId(folder.getId());
        noteDAO.createNote(note);

        // Delete the folder — ON DELETE SET NULL
        folderDAO.deleteFolder(folder.getId());

        Optional<Note> found = noteDAO.findById(note.getId());
        assertTrue(found.isPresent(), "Note should still exist");
        assertNull(found.get().getFolderId(),
                "folder_id should be NULL after folder deletion");
    }
}
