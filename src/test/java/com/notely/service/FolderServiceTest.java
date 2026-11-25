package com.notely.service;

import com.notely.dao.DatabaseConnection;
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
 * Integration tests for {@link FolderService}.
 *
 * Runs against the {@code notely_test} database.
 */
class FolderServiceTest {

    private final FolderService service = new FolderService();

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
    void createFolderPersistsAndReturns() throws SQLException {
        Folder folder = service.createFolder("Work");

        assertTrue(folder.getId() > 0);
        assertEquals("Work", folder.getName());
    }

    @Test
    void createFolderTrimsWhitespace() throws SQLException {
        Folder folder = service.createFolder("  Recipes  ");

        assertEquals("Recipes", folder.getName());
    }

    @Test
    void createFolderWithNullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createFolder(null));
    }

    @Test
    void createFolderWithBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createFolder("   "));
    }

    @Test
    void createFolderWithNameTooLongThrows() {
        String longName = "A".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> service.createFolder(longName));
    }

    @Test
    void createFolderWithNameAtMaxLengthSucceeds() throws SQLException {
        String maxName = "A".repeat(100);
        Folder folder = service.createFolder(maxName);
        assertEquals(maxName, folder.getName());
    }

    // ─────────────── renameFolder ─────────────────────────

    @Test
    void renameFolderUpdatesName() throws SQLException {
        Folder folder = service.createFolder("Old");

        Folder renamed = service.renameFolder(folder.getId(), "New");

        assertEquals("New", renamed.getName());
        Optional<Folder> found = service.getFolderById(folder.getId());
        assertTrue(found.isPresent());
        assertEquals("New", found.get().getName());
    }

    @Test
    void renameFolderTrimsWhitespace() throws SQLException {
        Folder folder = service.createFolder("Original");

        Folder renamed = service.renameFolder(folder.getId(), "  Trimmed  ");
        assertEquals("Trimmed", renamed.getName());
    }

    @Test
    void renameFolderNotFoundThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.renameFolder(9999, "Name"));
    }

    @Test
    void renameFolderWithBlankNameThrows() throws SQLException {
        Folder folder = service.createFolder("Valid");

        assertThrows(IllegalArgumentException.class,
                () -> service.renameFolder(folder.getId(), "  "));
    }

    @Test
    void renameFolderWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.renameFolder(0, "Name"));
    }

    @Test
    void renameFolderWithNegativeIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.renameFolder(-1, "Name"));
    }

    // ─────────────── deleteFolder ─────────────────────────

    @Test
    void deleteFolderRemovesIt() throws SQLException {
        Folder folder = service.createFolder("Doomed");

        service.deleteFolder(folder.getId());

        assertTrue(service.getFolderById(folder.getId()).isEmpty());
    }

    @Test
    void deleteFolderWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteFolder(0));
    }

    // ─────────────── getFolderById ────────────────────────

    @Test
    void getFolderByIdReturnsFolder() throws SQLException {
        Folder folder = service.createFolder("Found");

        Optional<Folder> found = service.getFolderById(folder.getId());
        assertTrue(found.isPresent());
        assertEquals("Found", found.get().getName());
    }

    @Test
    void getFolderByIdReturnsEmptyForMissing() throws SQLException {
        assertTrue(service.getFolderById(9999).isEmpty());
    }

    @Test
    void getFolderByIdWithInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getFolderById(0));
    }

    // ─────────────── getAllFolders ─────────────────────────

    @Test
    void getAllFoldersReturnsEmpty() throws SQLException {
        assertTrue(service.getAllFolders().isEmpty());
    }

    @Test
    void getAllFoldersReturnsSorted() throws SQLException {
        service.createFolder("Charlie");
        service.createFolder("Alpha");
        service.createFolder("Bravo");

        List<Folder> folders = service.getAllFolders();
        assertEquals(3, folders.size());
        assertEquals("Alpha", folders.get(0).getName());
        assertEquals("Bravo", folders.get(1).getName());
        assertEquals("Charlie", folders.get(2).getName());
    }
}
