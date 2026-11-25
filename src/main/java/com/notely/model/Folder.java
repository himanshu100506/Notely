package com.notely.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a folder that groups related notes.
 *
 * <h3>OOP concepts applied</h3>
 * <ul>
 *   <li><b>Encapsulation</b> — all fields are {@code private};
 *       the internal notes list is never leaked directly.</li>
 *   <li><b>Composition</b> — a Folder <em>has-a</em> list of Notes.
 *       The folder owns its notes conceptually; deleting a folder
 *       means its notes become un-foldered (handled by the DAO layer
 *       via {@code ON DELETE SET NULL}).</li>
 *   <li><b>Defensive copying</b> — {@link #getNotes()} returns an
 *       unmodifiable view so callers cannot corrupt the internal list.
 *       Notes are added/removed only through dedicated methods.</li>
 * </ul>
 *
 * <h3>Database mapping</h3>
 * Maps to the {@code folders} table (columns: id, name, created_at).
 */
public class Folder {

    // ────────────────────────── Fields ──────────────────────────

    /** Database primary key. */
    private int id;

    /** Display name, e.g. "Work", "Personal", "Recipes". */
    private String name;

    /** Timestamp when the folder was created. */
    private LocalDateTime createdAt;

    /**
     * Notes that belong to this folder.
     * Composition — the Folder owns this list.
     * Initialised eagerly to avoid null-checks everywhere.
     */
    private final List<Note> notes = new ArrayList<>();

    // ─────────────────────── Constructors ───────────────────────

    /**
     * No-arg constructor for framework use and pre-insert creation.
     */
    public Folder() {
    }

    /**
     * Convenience constructor — name only, id assigned by the database.
     *
     * @param name the folder display name
     */
    public Folder(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Full constructor — used when loading from the database.
     *
     * @param id        the database primary key
     * @param name      the folder display name
     * @param createdAt the timestamp from the database
     */
    public Folder(int id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    // ──────────────────── Getters and Setters ──────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ───────────────── Notes — composition helpers ─────────────

    /**
     * Returns a read-only view of the notes in this folder.
     * Callers cannot add or remove notes through this list;
     * use {@link #addNote} and {@link #removeNote} instead.
     *
     * @return unmodifiable list of notes
     */
    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * Adds a note to this folder if it is not already present.
     *
     * @param note the note to add
     */
    public void addNote(Note note) {
        if (note != null && !notes.contains(note)) {
            notes.add(note);
        }
    }

    /**
     * Removes a note from this folder.
     *
     * @param note the note to remove
     */
    public void removeNote(Note note) {
        notes.remove(note);
    }

    /**
     * Returns how many notes are in this folder.
     *
     * @return note count
     */
    public int getNoteCount() {
        return notes.size();
    }

    // ──────────────────────── toString ──────────────────────────

    /**
     * Example: {@code Folder{id=1, name='Work', notes=5}}
     */
    @Override
    public String toString() {
        return "Folder{id=" + id
                + ", name='" + name + "'"
                + ", notes=" + notes.size() + "}";
    }
}
