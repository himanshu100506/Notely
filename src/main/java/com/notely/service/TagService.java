package com.notely.service;

import com.notely.dao.TagDAO;
import com.notely.model.Tag;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for tag operations.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Input validation (name not blank, within length limit).</li>
 *   <li>Delegating persistence to {@link TagDAO}.</li>
 *   <li>Managing the note-tag relationship.</li>
 * </ul>
 */
public class TagService {

    private static final int MAX_TAG_NAME_LENGTH = 50;

    private final TagDAO tagDAO;

    public TagService() {
        this(new TagDAO());
    }

    public TagService(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    }

    // ────────────────────── Commands ──────────────────────

    public Tag createTag(String name) throws SQLException {
        validateTagName(name);
        Tag tag = new Tag(name.trim());
        tagDAO.createTag(tag);
        return tag;
    }

    public void deleteTag(int tagId) throws SQLException {
        validateId(tagId, "Tag ID");
        tagDAO.deleteTag(tagId);
    }

    public void addTagToNote(int noteId, int tagId) throws SQLException {
        validateId(noteId, "Note ID");
        validateId(tagId, "Tag ID");
        tagDAO.addTagToNote(noteId, tagId);
    }

    public void removeTagFromNote(int noteId, int tagId) throws SQLException {
        validateId(noteId, "Note ID");
        validateId(tagId, "Tag ID");
        tagDAO.removeTagFromNote(noteId, tagId);
    }

    // ────────────────────── Queries ───────────────────────

    public List<Tag> getAllTags() throws SQLException {
        return tagDAO.findAll();
    }

    public List<Tag> getTagsForNote(int noteId) throws SQLException {
        validateId(noteId, "Note ID");
        return tagDAO.findByNoteId(noteId);
    }

    // ────────────────────── Validation ────────────────────

    private void validateTagName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tag name must not be blank");
        }
        if (name.trim().length() > MAX_TAG_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Tag name must not exceed " + MAX_TAG_NAME_LENGTH
                            + " characters");
        }
    }

    private void validateId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }
}
