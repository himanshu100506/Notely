package com.notely.service;

import com.notely.dao.DatabaseConnection;
import com.notely.dao.NoteDAO;
import com.notely.model.Note;
import com.notely.model.Tag;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link TagService}.
 *
 * Runs against the {@code notely_test} database.
 */
class TagServiceTest {

    private final TagService service = new TagService();
    private final NoteDAO noteDAO = new NoteDAO();

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

    // ─────────────── createTag ────────────────────────────

    @Test
    void createTagPersistsAndReturns() throws SQLException {
        Tag tag = service.createTag("java");

        assertTrue(tag.getId() > 0);
        assertEquals("java", tag.getName());
    }

    @Test
    void createTagTrimsWhitespace() throws SQLException {
        Tag tag = service.createTag("  urgent  ");
        assertEquals("urgent", tag.getName());
    }

    @Test
    void createTagWithNullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTag(null));
    }

    @Test
    void createTagWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createTag("   "));
    }

    @Test
    void createTagWithNameTooLongThrows() {
        String longName = "A".repeat(51);
        assertThrows(IllegalArgumentException.class,
                () -> service.createTag(longName));
    }

    @Test
    void createTagWithNameAtMaxLengthSucceeds() throws SQLException {
        String maxName = "A".repeat(50);
        Tag tag = service.createTag(maxName);
        assertEquals(maxName, tag.getName());
    }

    // ─────────────── deleteTag ────────────────────────────

    @Test
    void deleteTagRemovesIt() throws SQLException {
        Tag tag = service.createTag("temporary");
        service.deleteTag(tag.getId());

        assertTrue(service.getAllTags().isEmpty());
    }

    @Test
    void deleteTagWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteTag(0));
    }

    @Test
    void deleteTagWithNegativeIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteTag(-5));
    }

    // ─────────────── getAllTags ───────────────────────────

    @Test
    void getAllTagsReturnsEmpty() throws SQLException {
        assertTrue(service.getAllTags().isEmpty());
    }

    @Test
    void getAllTagsReturnsSorted() throws SQLException {
        service.createTag("zebra");
        service.createTag("alpha");
        service.createTag("mango");

        List<Tag> tags = service.getAllTags();
        assertEquals(3, tags.size());
        assertEquals("alpha", tags.get(0).getName());
        assertEquals("mango", tags.get(1).getName());
        assertEquals("zebra", tags.get(2).getName());
    }

    // ─────────────── getTagsForNote ──────────────────────

    @Test
    void getTagsForNoteReturnsLinkedTags() throws SQLException {
        Note note = new Note("My Note", "content");
        noteDAO.createNote(note);
        Tag tag = service.createTag("important");
        service.addTagToNote(note.getId(), tag.getId());

        List<Tag> tags = service.getTagsForNote(note.getId());
        assertEquals(1, tags.size());
        assertEquals("important", tags.get(0).getName());
    }

    @Test
    void getTagsForNoteReturnsEmptyForUntagged() throws SQLException {
        Note note = new Note("Untagged", "content");
        noteDAO.createNote(note);

        assertTrue(service.getTagsForNote(note.getId()).isEmpty());
    }

    @Test
    void getTagsForNoteWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getTagsForNote(0));
    }

    // ─────────────── addTagToNote ─────────────────────────

    @Test
    void addTagToNoteCreatesLink() throws SQLException {
        Note note = new Note("Note", "content");
        noteDAO.createNote(note);
        Tag tag = service.createTag("linked");

        service.addTagToNote(note.getId(), tag.getId());

        List<Tag> tags = service.getTagsForNote(note.getId());
        assertEquals(1, tags.size());
    }

    @Test
    void addTagToNoteWithInvalidNoteIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addTagToNote(0, 1));
    }

    @Test
    void addTagToNoteWithInvalidTagIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addTagToNote(1, 0));
    }

    @Test
    void addTagToNoteWithNonExistentNoteThrowsSql() throws SQLException {
        Tag tag = service.createTag("orphan");
        assertThrows(SQLException.class,
                () -> service.addTagToNote(9999, tag.getId()));
    }

    // ─────────────── removeTagFromNote ────────────────────

    @Test
    void removeTagFromNoteDeletesLink() throws SQLException {
        Note note = new Note("Note", "content");
        noteDAO.createNote(note);
        Tag tag = service.createTag("removable");
        service.addTagToNote(note.getId(), tag.getId());

        service.removeTagFromNote(note.getId(), tag.getId());

        assertTrue(service.getTagsForNote(note.getId()).isEmpty());
    }

    @Test
    void removeTagFromNoteWithInvalidNoteIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.removeTagFromNote(0, 1));
    }

    @Test
    void removeTagFromNoteWithInvalidTagIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.removeTagFromNote(1, -1));
    }
}
