package com.notely.service;

import com.notely.dao.DatabaseConnection;
import com.notely.dao.FolderDAO;
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
 * Integration tests for {@link NoteService}.
 *
 * Runs against the {@code notely_test} database.
 */
class NoteServiceTest {

    private final NoteService service = new NoteService();
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
    void createNotePersistsAndReturns() throws SQLException {
        Note note = service.createNote("Title", "Content");

        assertTrue(note.getId() > 0);
        assertEquals("Title", note.getTitle());
        assertEquals("Content", note.getContent());
    }

    @Test
    void createNoteTrimsTitle() throws SQLException {
        Note note = service.createNote("  Trimmed  ", "body");
        assertEquals("Trimmed", note.getTitle());
    }

    @Test
    void createNoteWithNullContentDefaultsToEmpty() throws SQLException {
        Note note = service.createNote("Title", null);
        assertEquals("", note.getContent());
    }

    @Test
    void createNoteWithNullTitleThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createNote(null, "content"));
    }

    @Test
    void createNoteWithBlankTitleThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createNote("   ", "content"));
    }

    @Test
    void createNoteWithTitleTooLongThrows() {
        String longTitle = "A".repeat(201);
        assertThrows(IllegalArgumentException.class,
                () -> service.createNote(longTitle, "content"));
    }

    @Test
    void createNoteWithTitleAtMaxLengthSucceeds() throws SQLException {
        String maxTitle = "A".repeat(200);
        Note note = service.createNote(maxTitle, "content");
        assertEquals(maxTitle, note.getTitle());
    }

    // ─────────────── updateNote ───────────────────────────

    @Test
    void updateNoteChangesFields() throws SQLException {
        Note note = service.createNote("Original", "old");
        note.setTitle("Updated");
        note.setContent("new");

        service.updateNote(note);

        Optional<Note> found = service.getNoteById(note.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().getTitle());
        assertEquals("new", found.get().getContent());
    }

    @Test
    void updateNoteWithNullThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateNote(null));
    }

    @Test
    void updateNoteWithBlankTitleThrows() throws SQLException {
        Note note = service.createNote("Valid", "content");
        note.setTitle("   ");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateNote(note));
    }

    // ─────────────── deleteNote ───────────────────────────

    @Test
    void deleteNoteRemovesIt() throws SQLException {
        Note note = service.createNote("Doomed", "content");
        service.deleteNote(note.getId());

        assertTrue(service.getNoteById(note.getId()).isEmpty());
    }

    @Test
    void deleteNoteWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteNote(0));
    }

    @Test
    void deleteNoteWithNegativeIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteNote(-1));
    }

    // ─────────────── getNoteById ──────────────────────────

    @Test
    void getNoteByIdReturnsNote() throws SQLException {
        Note note = service.createNote("Found", "content");

        Optional<Note> found = service.getNoteById(note.getId());
        assertTrue(found.isPresent());
        assertEquals("Found", found.get().getTitle());
    }

    @Test
    void getNoteByIdReturnsEmptyForMissing() throws SQLException {
        assertTrue(service.getNoteById(9999).isEmpty());
    }

    @Test
    void getNoteByIdWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getNoteById(0));
    }

    // ─────────────── getAllNotes ──────────────────────────

    @Test
    void getAllNotesReturnsAll() throws SQLException {
        service.createNote("A", "a");
        service.createNote("B", "b");

        assertEquals(2, service.getAllNotes().size());
    }

    // ─────────────── getNotesByFolder ─────────────────────

    @Test
    void getNotesByFolderReturnsFiltered() throws SQLException {
        Folder folder = new Folder("Work");
        folderDAO.createFolder(folder);

        Note note = service.createNote("Work Note", "content");
        service.moveToFolder(note.getId(), folder.getId());
        service.createNote("Unfoldered", "content");

        List<Note> notes = service.getNotesByFolder(folder.getId());
        assertEquals(1, notes.size());
        assertEquals("Work Note", notes.get(0).getTitle());
    }

    @Test
    void getNotesByFolderWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getNotesByFolder(0));
    }

    // ─────────────── searchNotes ──────────────────────────

    @Test
    void searchNotesFindsMatch() throws SQLException {
        service.createNote("Java Tutorial", "Learn java programming");
        service.createNote("Cooking", "pasta recipe");

        List<Note> results = service.searchNotes("java");
        assertEquals(1, results.size());
        assertEquals("Java Tutorial", results.get(0).getTitle());
    }

    @Test
    void searchNotesWithNullReturnsEmpty() throws SQLException {
        assertEquals(List.of(), service.searchNotes(null));
    }

    @Test
    void searchNotesWithBlankReturnsEmpty() throws SQLException {
        assertEquals(List.of(), service.searchNotes("   "));
    }

    @Test
    void searchNotesTrimsInput() throws SQLException {
        service.createNote("Match", "database content");

        List<Note> results = service.searchNotes("  database  ");
        assertEquals(1, results.size());
    }

    // ─────────────── getPinnedNotes ───────────────────────

    @Test
    void getPinnedNotesReturnsOnlyPinned() throws SQLException {
        Note pinned = service.createNote("Important", "content");
        service.togglePin(pinned.getId());
        service.createNote("Regular", "content");

        List<Note> pinnedNotes = service.getPinnedNotes();
        assertEquals(1, pinnedNotes.size());
        assertTrue(pinnedNotes.get(0).isPinned());
    }

    // ─────────────── getRecentNotes ──────────────────────

    @Test
    void getRecentNotesRespectsLimit() throws SQLException {
        service.createNote("A", "a");
        service.createNote("B", "b");
        service.createNote("C", "c");

        assertEquals(2, service.getRecentNotes(2).size());
    }

    @Test
    void getRecentNotesWithZeroLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRecentNotes(0));
    }

    @Test
    void getRecentNotesWithNegativeLimitThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRecentNotes(-1));
    }

    // ─────────────── sorting ─────────────────────────────

    @Test
    void getNotesSortedByTitleReturnsAlphabetical() throws SQLException {
        service.createNote("Charlie", "c");
        service.createNote("Alpha", "a");
        service.createNote("Bravo", "b");

        List<Note> sorted = service.getNotesSortedByTitle();
        assertEquals("Alpha", sorted.get(0).getTitle());
        assertEquals("Bravo", sorted.get(1).getTitle());
        assertEquals("Charlie", sorted.get(2).getTitle());
    }

    @Test
    void getNotesSortedByDateReturnsAll() throws SQLException {
        service.createNote("First", "a");
        service.createNote("Second", "b");

        List<Note> sorted = service.getNotesSortedByDate();
        assertEquals(2, sorted.size());
    }

    // ─────────────── togglePin ────────────────────────────

    @Test
    void togglePinFlipsFalseToTrue() throws SQLException {
        Note note = service.createNote("Note", "content");
        assertFalse(note.isPinned());

        Note toggled = service.togglePin(note.getId());
        assertTrue(toggled.isPinned());

        Optional<Note> found = service.getNoteById(note.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isPinned());
    }

    @Test
    void togglePinFlipsTrueToFalse() throws SQLException {
        Note note = service.createNote("Note", "content");
        service.togglePin(note.getId());

        Note toggled = service.togglePin(note.getId());
        assertFalse(toggled.isPinned());
    }

    @Test
    void togglePinNotFoundThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.togglePin(9999));
    }

    @Test
    void togglePinWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.togglePin(0));
    }

    // ─────────────── moveToFolder ─────────────────────────

    @Test
    void moveToFolderAssignsFolder() throws SQLException {
        Folder folder = new Folder("Dest");
        folderDAO.createFolder(folder);
        Note note = service.createNote("Note", "content");

        Note moved = service.moveToFolder(note.getId(), folder.getId());
        assertEquals(folder.getId(), moved.getFolderId());

        Optional<Note> found = service.getNoteById(note.getId());
        assertTrue(found.isPresent());
        assertEquals(folder.getId(), found.get().getFolderId());
    }

    @Test
    void moveToFolderWithNullUnassigns() throws SQLException {
        Folder folder = new Folder("Temp");
        folderDAO.createFolder(folder);
        Note note = service.createNote("Note", "content");
        service.moveToFolder(note.getId(), folder.getId());

        Note moved = service.moveToFolder(note.getId(), null);
        assertNull(moved.getFolderId());
    }

    @Test
    void moveToFolderNotFoundThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.moveToFolder(9999, 1));
    }

    @Test
    void moveToFolderWithInvalidNoteIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.moveToFolder(0, 1));
    }

    @Test
    void moveToFolderWithInvalidFolderIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.moveToFolder(1, -5));
    }

    // ─────────────── getKeywordSuggestions ────────────────

    @Test
    void getKeywordSuggestionsReturnsKeywords() throws SQLException {
        Note note = service.createNote(
                "Java Programming",
                "Learn java programming with spring framework spring");

        List<String> keywords = service.getKeywordSuggestions(note.getId(), 3);

        assertFalse(keywords.isEmpty());
        assertTrue(keywords.contains("spring"),
                "spring appears twice, should be a top keyword");
    }

    @Test
    void getKeywordSuggestionsNotFoundThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getKeywordSuggestions(9999, 5));
    }

    @Test
    void getKeywordSuggestionsWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getKeywordSuggestions(0, 5));
    }

    @Test
    void getKeywordSuggestionsWithZeroMaxThrows() throws SQLException {
        Note note = service.createNote("Note", "content");
        assertThrows(IllegalArgumentException.class,
                () -> service.getKeywordSuggestions(note.getId(), 0));
    }

    // ─────────────── getRelatedKeywords ──────────────────

    @Test
    void getRelatedKeywordsFindsRelatedTerms() throws SQLException {
        service.createNote("Java Web", "java spring framework");
        service.createNote("Java API", "java spring rest endpoints");
        service.createNote("Python", "python scripting guide");

        List<String> related = service.getRelatedKeywords("java", 5);

        assertTrue(related.contains("spring"),
                "spring co-occurs with java in two notes");
        assertFalse(related.contains("java"),
                "The search keyword itself must be excluded");
        assertFalse(related.contains("python"),
                "Words from non-matching notes should not appear");
    }

    @Test
    void getRelatedKeywordsWithNullReturnsEmpty() throws SQLException {
        assertEquals(List.of(), service.getRelatedKeywords(null, 5));
    }

    @Test
    void getRelatedKeywordsWithBlankReturnsEmpty() throws SQLException {
        assertEquals(List.of(), service.getRelatedKeywords("  ", 5));
    }

    @Test
    void getRelatedKeywordsWithZeroMaxThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getRelatedKeywords("java", 0));
    }
}
