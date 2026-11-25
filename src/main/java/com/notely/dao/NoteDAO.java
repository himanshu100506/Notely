package com.notely.dao;

import com.notely.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code notes} table.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Each method obtains its own {@link Connection} and closes it
 *       via try-with-resources.</li>
 *   <li>All queries use {@link PreparedStatement} to prevent
 *       SQL injection.</li>
 *   <li>{@code folder_id} is nullable — when the Java
 *       {@link Note#getFolderId()} is {@code null}, we call
 *       {@code setNull()} instead of {@code setInt()}.</li>
 *   <li>No business logic — sorting direction, search wildcards
 *       and limits are query concerns, not application logic.</li>
 * </ul>
 */
public class NoteDAO {

    // ────────────────────── SQL constants ──────────────────────

    private static final String INSERT_SQL =
            "INSERT INTO notes (title, content, is_pinned, folder_id, "
                    + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE notes SET title = ?, content = ?, is_pinned = ?, "
                    + "folder_id = ? WHERE id = ?";
    // updated_at is handled by MySQL ON UPDATE CURRENT_TIMESTAMP

    private static final String DELETE_SQL =
            "DELETE FROM notes WHERE id = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "ORDER BY updated_at DESC";

    private static final String SELECT_BY_FOLDER_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "WHERE folder_id = ? ORDER BY updated_at DESC";

    private static final String SEARCH_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "WHERE title LIKE ? OR content LIKE ? "
                    + "ORDER BY updated_at DESC";

    private static final String SELECT_PINNED_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "WHERE is_pinned = TRUE ORDER BY updated_at DESC";

    private static final String SELECT_RECENT_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "ORDER BY updated_at DESC LIMIT ?";

    private static final String SELECT_SORT_TITLE_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "ORDER BY title ASC";

    private static final String SELECT_SORT_DATE_SQL =
            "SELECT id, title, content, is_pinned, folder_id, "
                    + "created_at, updated_at FROM notes "
                    + "ORDER BY updated_at DESC";

    // ────────────────────── CRUD methods ──────────────────────

    /**
     * Inserts a new note and sets its generated {@code id}.
     *
     * <p>If {@link Note#getFolderId()} is {@code null}, the database
     * column {@code folder_id} is set to {@code NULL} (no folder).</p>
     *
     * @param note the note to insert (id is set on return)
     * @throws SQLException if the insert fails
     */
    public void createNote(Note note) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setBoolean(3, note.isPinned());
            setNullableInt(ps, 4, note.getFolderId());
            ps.setTimestamp(5, Timestamp.valueOf(note.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.valueOf(note.getUpdatedAt()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    note.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Updates an existing note's title, content, pinned state and
     * folder assignment.
     *
     * <p>The {@code updated_at} column is refreshed automatically
     * by MySQL ({@code ON UPDATE CURRENT_TIMESTAMP}).</p>
     *
     * @param note the note with updated fields
     * @throws SQLException if the update fails
     */
    public void updateNote(Note note) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {

            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setBoolean(3, note.isPinned());
            setNullableInt(ps, 4, note.getFolderId());
            ps.setInt(5, note.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a note by id.
     * Related rows in {@code note_tags} are removed automatically
     * ({@code ON DELETE CASCADE}).
     *
     * @param noteId the id of the note to delete
     * @throws SQLException if the delete fails
     */
    public void deleteNote(int noteId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {

            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
    }

    // ────────────────────── Query methods ─────────────────────

    /**
     * Finds a single note by its primary key.
     *
     * @param id the note id
     * @return an {@link Optional} containing the note, or empty
     * @throws SQLException if the query fails
     */
    public Optional<Note> findById(int id) throws SQLException {
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
     * Returns all notes ordered by last-updated descending.
     *
     * @return list of all notes
     * @throws SQLException if the query fails
     */
    public List<Note> findAll() throws SQLException {
        return executeQuery(SELECT_ALL_SQL);
    }

    /**
     * Returns all notes belonging to a specific folder.
     *
     * @param folderId the folder id
     * @return list of notes in that folder
     * @throws SQLException if the query fails
     */
    public List<Note> findByFolder(int folderId) throws SQLException {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_FOLDER_SQL)) {

            ps.setInt(1, folderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    /**
     * Searches notes whose title or content contains the keyword.
     * The search is case-insensitive (MySQL default collation).
     *
     * @param keyword the search term
     * @return matching notes
     * @throws SQLException if the query fails
     */
    public List<Note> searchByTitleOrContent(String keyword) throws SQLException {
        List<Note> notes = new ArrayList<>();
        String pattern = "%" + keyword + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SEARCH_SQL)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    /**
     * Returns all pinned notes, most recently updated first.
     *
     * @return list of pinned notes
     * @throws SQLException if the query fails
     */
    public List<Note> findPinned() throws SQLException {
        return executeQuery(SELECT_PINNED_SQL);
    }

    /**
     * Returns the N most recently updated notes.
     *
     * @param limit maximum number of notes to return
     * @return list of recent notes
     * @throws SQLException if the query fails
     */
    public List<Note> findRecent(int limit) throws SQLException {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RECENT_SQL)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    /**
     * Returns all notes sorted alphabetically by title (A → Z).
     *
     * @return sorted list of notes
     * @throws SQLException if the query fails
     */
    public List<Note> sortByTitle() throws SQLException {
        return executeQuery(SELECT_SORT_TITLE_SQL);
    }

    /**
     * Returns all notes sorted by last-updated date (newest first).
     *
     * @return sorted list of notes
     * @throws SQLException if the query fails
     */
    public List<Note> sortByDate() throws SQLException {
        return executeQuery(SELECT_SORT_DATE_SQL);
    }

    // ───────────────── Helper methods ─────────────────────────

    /**
     * Executes a parameterless SELECT and returns the result list.
     * Used by queries that need no bind parameters.
     */
    private List<Note> executeQuery(String sql) throws SQLException {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    /**
     * Maps the current ResultSet row to a {@link Note} object.
     *
     * <p>Handles the nullable {@code folder_id} column: if SQL
     * {@code NULL}, the Java field is set to {@code null}.</p>
     */
    private Note mapRow(ResultSet rs) throws SQLException {
        int folderId = rs.getInt("folder_id");
        Integer folderIdBoxed = rs.wasNull() ? null : folderId;

        return new Note(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getBoolean("is_pinned"),
                folderIdBoxed,
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    /**
     * Sets an {@code INT} parameter that may be {@code null}.
     * If the Java value is {@code null}, sends SQL {@code NULL};
     * otherwise sends the int value.
     *
     * @param ps    the prepared statement
     * @param index the parameter index (1-based)
     * @param value the Integer value, possibly null
     */
    private void setNullableInt(PreparedStatement ps, int index,
                                Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
