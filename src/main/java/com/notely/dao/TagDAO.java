package com.notely.dao;

import com.notely.model.Tag;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the {@code tags} and {@code note_tags} tables.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Each method uses its own {@link Connection} via try-with-resources.</li>
 *   <li>{@link #addTagToNote} uses a <b>transaction</b> because it
 *       combines a validation check (both FK targets must exist) with
 *       an insert into the join table — these must be atomic.</li>
 *   <li>All queries use {@link PreparedStatement}.</li>
 * </ul>
 */
public class TagDAO {

    // ────────────────────── SQL constants ──────────────────────

    private static final String INSERT_SQL =
            "INSERT INTO tags (name) VALUES (?)";

    private static final String DELETE_SQL =
            "DELETE FROM tags WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, name FROM tags ORDER BY name ASC";

    private static final String SELECT_BY_NOTE_SQL =
            "SELECT t.id, t.name FROM tags t "
                    + "JOIN note_tags nt ON t.id = nt.tag_id "
                    + "WHERE nt.note_id = ? ORDER BY t.name ASC";

    private static final String INSERT_NOTE_TAG_SQL =
            "INSERT INTO note_tags (note_id, tag_id) VALUES (?, ?)";

    private static final String DELETE_NOTE_TAG_SQL =
            "DELETE FROM note_tags WHERE note_id = ? AND tag_id = ?";

    private static final String NOTE_EXISTS_SQL =
            "SELECT 1 FROM notes WHERE id = ?";

    private static final String TAG_EXISTS_SQL =
            "SELECT 1 FROM tags WHERE id = ?";

    // ────────────────────── CRUD methods ──────────────────────

    /**
     * Inserts a new tag and sets its generated {@code id}.
     *
     * @param tag the tag to insert
     * @throws SQLException if the insert fails (e.g. duplicate name)
     */
    public void createTag(Tag tag) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tag.getName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tag.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Deletes a tag by id.
     * Related rows in {@code note_tags} are removed automatically
     * by the database ({@code ON DELETE CASCADE}).
     *
     * @param tagId the id of the tag to delete
     * @throws SQLException if the delete fails
     */
    public void deleteTag(int tagId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {

            ps.setInt(1, tagId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns all tags ordered by name (A → Z).
     *
     * @return list of all tags
     * @throws SQLException if the query fails
     */
    public List<Tag> findAll() throws SQLException {
        List<Tag> tags = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tags.add(mapRow(rs));
            }
        }
        return tags;
    }

    /**
     * Returns all tags attached to a specific note.
     *
     * @param noteId the note id
     * @return list of tags for that note
     * @throws SQLException if the query fails
     */
    public List<Tag> findByNoteId(int noteId) throws SQLException {
        List<Tag> tags = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_NOTE_SQL)) {

            ps.setInt(1, noteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapRow(rs));
                }
            }
        }
        return tags;
    }

    // ───────────── Note-Tag join table operations ─────────────

    /**
     * Links a tag to a note (inserts into the {@code note_tags}
     * join table).
     *
     * <p>Uses a <b>transaction</b>: the method first verifies that
     * both the note and the tag exist, then inserts the link.
     * If any step fails, the entire operation is rolled back.</p>
     *
     * @param noteId the note id
     * @param tagId  the tag id
     * @throws SQLException if the operation fails or either FK
     *                      target does not exist
     */
    public void addTagToNote(int noteId, int tagId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);          // BEGIN TRANSACTION

            // Verify note exists
            if (!recordExists(conn, NOTE_EXISTS_SQL, noteId)) {
                throw new SQLException(
                        "Note with id " + noteId + " does not exist");
            }
            // Verify tag exists
            if (!recordExists(conn, TAG_EXISTS_SQL, tagId)) {
                throw new SQLException(
                        "Tag with id " + tagId + " does not exist");
            }

            // Insert the link
            try (PreparedStatement ps =
                         conn.prepareStatement(INSERT_NOTE_TAG_SQL)) {
                ps.setInt(1, noteId);
                ps.setInt(2, tagId);
                ps.executeUpdate();
            }

            conn.commit();                      // COMMIT

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) { }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) { }
                try { conn.close(); } catch (SQLException ignored) { }
            }
        }
    }

    /**
     * Removes the link between a note and a tag.
     *
     * @param noteId the note id
     * @param tagId  the tag id
     * @throws SQLException if the delete fails
     */
    public void removeTagFromNote(int noteId, int tagId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_NOTE_TAG_SQL)) {

            ps.setInt(1, noteId);
            ps.setInt(2, tagId);
            ps.executeUpdate();
        }
    }

    // ───────────────── Helper methods ─────────────────────────

    /**
     * Checks whether a record exists using the given SQL
     * (must be a {@code SELECT 1 ... WHERE id = ?} query).
     */
    private boolean recordExists(Connection conn, String sql, int id)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Maps the current ResultSet row to a {@link Tag} object. */
    private Tag mapRow(ResultSet rs) throws SQLException {
        return new Tag(
                rs.getInt("id"),
                rs.getString("name")
        );
    }
}
