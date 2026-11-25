package com.notely.dao;

import com.notely.model.Folder;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link FolderDAO}.
 *
 * Runs against the {@code notely_test} database.
 * Each test starts with empty tables ({@code @BeforeEach} truncates).
 */
class FolderDAOTest {

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

    // ─────────────── createFolder ─────────────────────────

    @Test
    void createFolderSetsGeneratedId() throws SQLException {
        Folder folder = new Folder("Work");
        folderDAO.createFolder(folder);

        assertTrue(folder.getId() > 0,
                "ID should be set by the database after insert");
    }

    @Test
    void createFolderPersistsName() throws SQLException {
        Folder folder = new Folder("Personal");
        folderDAO.createFolder(folder);

        Optional<Folder> found = folderDAO.findById(folder.getId());
        assertTrue(found.isPresent());
        assertEquals("Personal", found.get().getName());
    }

    // ─────────────── updateFolder ─────────────────────────

    @Test
    void updateFolderChangesName() throws SQLException {
        Folder folder = new Folder("Old Name");
        folderDAO.createFolder(folder);

        folder.setName("New Name");
        folderDAO.updateFolder(folder);

        Optional<Folder> found = folderDAO.findById(folder.getId());
        assertTrue(found.isPresent());
        assertEquals("New Name", found.get().getName());
    }

    // ─────────────── deleteFolder ─────────────────────────

    @Test
    void deleteFolderRemovesIt() throws SQLException {
        Folder folder = new Folder("Temporary");
        folderDAO.createFolder(folder);

        folderDAO.deleteFolder(folder.getId());

        Optional<Folder> found = folderDAO.findById(folder.getId());
        assertTrue(found.isEmpty(), "Folder should be gone after delete");
    }

    // ─────────────── findById ─────────────────────────────

    @Test
    void findByIdReturnsEmptyForMissingId() throws SQLException {
        Optional<Folder> found = folderDAO.findById(9999);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdReturnsCorrectFolder() throws SQLException {
        Folder a = new Folder("Alpha");
        Folder b = new Folder("Beta");
        folderDAO.createFolder(a);
        folderDAO.createFolder(b);

        Optional<Folder> found = folderDAO.findById(b.getId());
        assertTrue(found.isPresent());
        assertEquals("Beta", found.get().getName());
    }

    // ─────────────── findAll ──────────────────────────────

    @Test
    void findAllReturnsEmptyListWhenNoFolders() throws SQLException {
        List<Folder> folders = folderDAO.findAll();
        assertTrue(folders.isEmpty());
    }

    @Test
    void findAllReturnsFoldersOrderedByName() throws SQLException {
        folderDAO.createFolder(new Folder("Charlie"));
        folderDAO.createFolder(new Folder("Alpha"));
        folderDAO.createFolder(new Folder("Bravo"));

        List<Folder> folders = folderDAO.findAll();
        assertEquals(3, folders.size());
        assertEquals("Alpha", folders.get(0).getName());
        assertEquals("Bravo", folders.get(1).getName());
        assertEquals("Charlie", folders.get(2).getName());
    }

    // ─────────────── Timestamp ────────────────────────────

    @Test
    void createdAtIsPersisted() throws SQLException {
        Folder folder = new Folder("Timestamped");
        folderDAO.createFolder(folder);

        Optional<Folder> found = folderDAO.findById(folder.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getCreatedAt(),
                "created_at should be loaded from the database");
    }
}
