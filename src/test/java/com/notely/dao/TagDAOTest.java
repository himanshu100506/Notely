package com.notely.dao;

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
 * Integration tests for {@link TagDAO}.
 *
 * Runs against the {@code notely_test} database.
 * Each test starts with empty tables.
 */
class TagDAOTest {

    private final TagDAO tagDAO = new TagDAO();
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
    void createTagSetsGeneratedId() throws SQLException {
        Tag tag = new Tag("java");
        tagDAO.createTag(tag);

        assertTrue(tag.getId() > 0,
                "ID should be set by the database after insert");
    }

    @Test
    void createTagPersistsName() throws SQLException {
        Tag tag = new Tag("urgent");
        tagDAO.createTag(tag);

        List<Tag> all = tagDAO.findAll();
        assertEquals(1, all.size());
        assertEquals("urgent", all.get(0).getName());
    }

    @Test
    void createDuplicateTagThrows() throws SQLException {
        tagDAO.createTag(new Tag("unique"));

        assertThrows(SQLException.class, () -> tagDAO.createTag(new Tag("unique")),
                "Duplicate tag name should violate UNIQUE constraint");
    }

    // ─────────────── deleteTag ────────────────────────────

    @Test
    void deleteTagRemovesIt() throws SQLException {
        Tag tag = new Tag("temporary");
        tagDAO.createTag(tag);

        tagDAO.deleteTag(tag.getId());

        List<Tag> all = tagDAO.findAll();
        assertTrue(all.isEmpty(), "Tag should be gone after delete");
    }

    @Test
    void deleteTagCascadesToNoteTagsJoinTable() throws SQLException {
        Note note = new Note("Tagged Note", "content");
        noteDAO.createNote(note);
        Tag tag = new Tag("doomed");
        tagDAO.createTag(tag);
        tagDAO.addTagToNote(note.getId(), tag.getId());

        tagDAO.deleteTag(tag.getId());

        List<Tag> tagsOnNote = tagDAO.findByNoteId(note.getId());
        assertTrue(tagsOnNote.isEmpty(),
                "note_tags row should be removed by ON DELETE CASCADE");
    }

    // ─────────────── findAll ─────────────────────────────

    @Test
    void findAllReturnsEmptyWhenNoTags() throws SQLException {
        List<Tag> tags = tagDAO.findAll();
        assertTrue(tags.isEmpty());
    }

    @Test
    void findAllReturnsTagsOrderedByName() throws SQLException {
        tagDAO.createTag(new Tag("charlie"));
        tagDAO.createTag(new Tag("alpha"));
        tagDAO.createTag(new Tag("bravo"));

        List<Tag> tags = tagDAO.findAll();
        assertEquals(3, tags.size());
        assertEquals("alpha", tags.get(0).getName());
        assertEquals("bravo", tags.get(1).getName());
        assertEquals("charlie", tags.get(2).getName());
    }

    // ─────────────── findByNoteId ────────────────────────

    @Test
    void findByNoteIdReturnsOnlyLinkedTags() throws SQLException {
        Note note1 = new Note("Note A", "content");
        Note note2 = new Note("Note B", "content");
        noteDAO.createNote(note1);
        noteDAO.createNote(note2);

        Tag java = new Tag("java");
        Tag python = new Tag("python");
        tagDAO.createTag(java);
        tagDAO.createTag(python);

        tagDAO.addTagToNote(note1.getId(), java.getId());
        tagDAO.addTagToNote(note2.getId(), python.getId());

        List<Tag> note1Tags = tagDAO.findByNoteId(note1.getId());
        assertEquals(1, note1Tags.size());
        assertEquals("java", note1Tags.get(0).getName());
    }

    @Test
    void findByNoteIdReturnsEmptyForUntaggedNote() throws SQLException {
        Note note = new Note("Untagged", "content");
        noteDAO.createNote(note);

        List<Tag> tags = tagDAO.findByNoteId(note.getId());
        assertTrue(tags.isEmpty());
    }

    // ─────────────── addTagToNote ─────────────────────────

    @Test
    void addTagToNoteCreatesLink() throws SQLException {
        Note note = new Note("My Note", "content");
        noteDAO.createNote(note);
        Tag tag = new Tag("important");
        tagDAO.createTag(tag);

        tagDAO.addTagToNote(note.getId(), tag.getId());

        List<Tag> tags = tagDAO.findByNoteId(note.getId());
        assertEquals(1, tags.size());
        assertEquals("important", tags.get(0).getName());
    }

    @Test
    void addTagToNoteThrowsForMissingNote() throws SQLException {
        Tag tag = new Tag("orphan");
        tagDAO.createTag(tag);

        assertThrows(SQLException.class,
                () -> tagDAO.addTagToNote(9999, tag.getId()),
                "Should fail when note does not exist");
    }

    @Test
    void addTagToNoteThrowsForMissingTag() throws SQLException {
        Note note = new Note("Lonely", "content");
        noteDAO.createNote(note);

        assertThrows(SQLException.class,
                () -> tagDAO.addTagToNote(note.getId(), 9999),
                "Should fail when tag does not exist");
    }

    // ─────────────── removeTagFromNote ────────────────────

    @Test
    void removeTagFromNoteDeletesLink() throws SQLException {
        Note note = new Note("Note", "content");
        noteDAO.createNote(note);
        Tag tag = new Tag("removable");
        tagDAO.createTag(tag);
        tagDAO.addTagToNote(note.getId(), tag.getId());

        tagDAO.removeTagFromNote(note.getId(), tag.getId());

        List<Tag> tags = tagDAO.findByNoteId(note.getId());
        assertTrue(tags.isEmpty(), "Tag link should be removed");
    }

    @Test
    void removeTagFromNoteDoesNotDeleteTheTagItself() throws SQLException {
        Note note = new Note("Note", "content");
        noteDAO.createNote(note);
        Tag tag = new Tag("survives");
        tagDAO.createTag(tag);
        tagDAO.addTagToNote(note.getId(), tag.getId());

        tagDAO.removeTagFromNote(note.getId(), tag.getId());

        List<Tag> allTags = tagDAO.findAll();
        assertEquals(1, allTags.size(),
                "The tag itself should still exist after unlinking");
        assertEquals("survives", allTags.get(0).getName());
    }
}
