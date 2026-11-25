package com.notely.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single note in the knowledge base.
 *
 * <h3>OOP concepts applied</h3>
 * <ul>
 *   <li><b>Encapsulation</b> — every field is {@code private};
 *       external code interacts only through getters/setters.</li>
 *   <li><b>Association</b> — a Note belongs to one {@link Folder}
 *       (tracked by {@code folderId}, a foreign-key reference).
 *       This is a many-to-one relationship.</li>
 *   <li><b>HashSet for tags</b> — tags are stored in a
 *       {@code HashSet<Tag>} to prevent duplicates and provide
 *       O(1) lookup.  This relies on {@link Tag#equals} and
 *       {@link Tag#hashCode} being correctly overridden.</li>
 *   <li><b>Defensive copying</b> — {@link #getTags()} returns an
 *       unmodifiable view; tags are added/removed only through
 *       dedicated methods.</li>
 * </ul>
 *
 * <h3>Database mapping</h3>
 * Maps to the {@code notes} table.  The many-to-many relationship
 * with tags is stored in the {@code note_tags} join table.
 */
public class Note {

    // ────────────────────────── Fields ──────────────────────────

    /** Database primary key. */
    private int id;

    /** Short headline for the note (shown in the note list). */
    private String title;

    /** Full body text of the note. */
    private String content;

    /** Whether this note is pinned to the top of the list. */
    private boolean pinned;

    /**
     * Foreign key to the folder this note belongs to.
     * {@code null} means the note is not assigned to any folder,
     * which maps directly to SQL {@code NULL} in the database.
     */
    private Integer folderId;

    /** When the note was first created. */
    private LocalDateTime createdAt;

    /** When the note was last modified. */
    private LocalDateTime updatedAt;

    /**
     * Tags attached to this note.
     * HashSet — duplicates are impossible, and contains/add/remove
     * are all O(1) on average.
     */
    private final Set<Tag> tags = new HashSet<>();

    // ─────────────────────── Constructors ───────────────────────

    /**
     * No-arg constructor for framework use and pre-insert creation.
     */
    public Note() {
    }

    /**
     * Convenience constructor for creating a brand-new note.
     * Timestamps are set to "now"; the note starts un-pinned and
     * with no folder.
     *
     * @param title   the note headline
     * @param content the note body
     */
    public Note(String title, String content) {
        this.title = title;
        this.content = content;
        this.pinned = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Full constructor — used when loading a complete row from the database.
     *
     * @param id        database primary key
     * @param title     note headline
     * @param content   note body
     * @param pinned    pin state
     * @param folderId  owning folder ({@code null} = none)
     * @param createdAt creation timestamp
     * @param updatedAt last-modified timestamp
     */
    public Note(int id, String title, String content, boolean pinned,
                Integer folderId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.folderId = folderId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ──────────────────── Getters and Setters ──────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ──────────────────── Tags — HashSet helpers ───────────────

    /**
     * Returns a read-only view of this note's tags.
     *
     * @return unmodifiable set of tags
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Attaches a tag to this note.
     * Because {@code tags} is a HashSet, adding the same Tag twice
     * (same {@code id}) is silently ignored — no duplicates.
     *
     * @param tag the tag to attach
     */
    public void addTag(Tag tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    /**
     * Detaches a tag from this note.
     *
     * @param tag the tag to remove
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    /**
     * Checks whether this note carries a specific tag.
     *
     * @param tag the tag to look for
     * @return true if present
     */
    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    // ──────────────────────── toString ──────────────────────────

    /**
     * Example: {@code Note{id=1, title='Meeting notes', pinned=true, tags=2}}
     */
    @Override
    public String toString() {
        return "Note{id=" + id
                + ", title='" + title + "'"
                + ", pinned=" + pinned
                + ", tags=" + tags.size() + "}";
    }
}
