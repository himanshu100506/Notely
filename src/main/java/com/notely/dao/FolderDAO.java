package com.notely.dao;

import com.notely.model.Folder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code folders} table.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Every method obtains its own {@link Connection} via
 *       {@link DatabaseConnection#getConnection()} and closes it
 *       with try-with-resources.</li>
 *   <li>All queries use {@link PreparedStatement} to prevent
 *       SQL injection.</li>
 *   <li>No business logic — this class only translates between
 *       Java objects and SQL rows.</li>
 * </ul>
 */
public class FolderDAO {

    // ────────────────────── SQL constants ──────────────────────

    private static final String INSERT_SQL =
            "INSERT INTO folders (name, created_at) VALUES (?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE folders SET name = ? WHERE id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM folders WHERE id = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, name, created_at FROM folders WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, name, created_at FROM folders ORDER BY name ASC";

    // ────────────────────── CRUD methods ──────────────────────

    /**
     * Inserts a new folder and sets its generated {@code id}.
     *
     * @param folder the folder to insert (id field is set on return)
     * @throws SQLException if the insert fails
     */
    public void createFolder(Folder folder) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, folder.getName());
            ps.setTimestamp(2, Timestamp.valueOf(folder.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    folder.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates the name of an existing folder.
     *
     * @param folder the folder with the updated name
     * @throws SQLException if the update fails
     */
    public void updateFolder(Folder folder) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {

            ps.setString(1, folder.getName());
            ps.setInt(2, folder.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a folder by id.
     * Notes in this folder have their {@code folder_id} set to
     * {@code NULL} automatically by the database ({@code ON DELETE SET NULL}).
     *
     * @param folderId the id of the folder to delete
     * @throws SQLException if the delete fails
     */
    public void deleteFolder(int folderId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {

            ps.setInt(1, folderId);
            ps.executeUpdate();
        }
    }

    /**
     * Finds a single folder by its primary key.
     *
     * @param id the folder id
     * @return an {@link Optional} containing the folder, or empty
     * @throws SQLException if the query fails
     */
    public Optional<Folder> findById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all folders ordered by name (A → Z).
     *
     * @return list of all folders
     * @throws SQLException if the query fails
     */
    public List<Folder> findAll() throws SQLException {
        List<Folder> folders = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                folders.add(mapRow(rs));
            }
        }
        return folders;
    }

    // ───────────────── Row-mapping helper ─────────────────────

    /**
     * Maps the current ResultSet row to a {@link Folder} object.
     * Centralised here so every query uses the same mapping logic.
     */
    private Folder mapRow(ResultSet rs) throws SQLException {
        return new Folder(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
