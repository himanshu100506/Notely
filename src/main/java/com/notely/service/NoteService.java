package com.notely.service;

import com.notely.dao.NoteDAO;
import com.notely.model.Note;
import com.notely.util.KeywordExtractor;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for note operations.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Input validation (title non-blank, IDs positive, limits sane).</li>
 *   <li>CRUD orchestration through {@link NoteDAO}.</li>
 *   <li>Search, sorting, pin management, folder assignment.</li>
 *   <li>Keyword extraction via {@link KeywordExtractor}.</li>
 * </ul>
 *
 * <h3>Dependency injection</h3>
 * The DAO and keyword extractor are accepted via the constructor.
 * A no-arg constructor provides the default production wiring.
 */
public class NoteService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final NoteDAO noteDAO;
    private final KeywordExtractor keywordExtractor;

    public NoteService() {
        this(new NoteDAO(), new KeywordExtractor());
    }

    public NoteService(NoteDAO noteDAO, KeywordExtractor keywordExtractor) {
        this.noteDAO = noteDAO;
        this.keywordExtractor = keywordExtractor;
    }

    // ────────────────────── Commands ──────────────────────

    public Note createNote(String title, String content) throws SQLException {
        validateTitle(title);
        Note note = new Note(title.trim(), content != null ? content : "");
        noteDAO.createNote(note);
        return note;
    }

    public void updateNote(Note note) throws SQLException {
        if (note == null) {
            throw new IllegalArgumentException("Note must not be null");
        }
        validateTitle(note.getTitle());
        noteDAO.updateNote(note);
    }

    public void deleteNote(int noteId) throws SQLException {
        validateId(noteId, "Note ID");
        noteDAO.deleteNote(noteId);
    }

    public Note togglePin(int noteId) throws SQLException {
        validateId(noteId, "Note ID");
        Note note = noteDAO.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note not found: " + noteId));
        note.setPinned(!note.isPinned());
        noteDAO.updateNote(note);
        return note;
    }

    public Note moveToFolder(int noteId, Integer folderId) throws SQLException {
        validateId(noteId, "Note ID");
        if (folderId != null && folderId <= 0) {
            throw new IllegalArgumentException("Folder ID must be positive");
        }
        Note note = noteDAO.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note not found: " + noteId));
        note.setFolderId(folderId);
        noteDAO.updateNote(note);
        return note;
    }

    // ────────────────────── Queries ───────────────────────

    public Optional<Note> getNoteById(int id) throws SQLException {
        validateId(id, "Note ID");
        return noteDAO.findById(id);
    }

    public List<Note> getAllNotes() throws SQLException {
        return noteDAO.findAll();
    }

    public List<Note> getNotesByFolder(int folderId) throws SQLException {
        validateId(folderId, "Folder ID");
        return noteDAO.findByFolder(folderId);
    }

    public List<Note> searchNotes(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return noteDAO.searchByTitleOrContent(keyword.trim());
    }

    public List<Note> getPinnedNotes() throws SQLException {
        return noteDAO.findPinned();
    }

    public List<Note> getRecentNotes(int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return noteDAO.findRecent(limit);
    }

    public List<Note> getNotesSortedByTitle() throws SQLException {
        return noteDAO.sortByTitle();
    }

    public List<Note> getNotesSortedByDate() throws SQLException {
        return noteDAO.sortByDate();
    }

    // ──────────────── Keyword extraction ──────────────────

    public List<String> getKeywordSuggestions(int noteId, int maxKeywords)
            throws SQLException {
        validateId(noteId, "Note ID");
        if (maxKeywords <= 0) {
            throw new IllegalArgumentException("maxKeywords must be positive");
        }
        Note note = noteDAO.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Note not found: " + noteId));
        String text = combine(note.getTitle(), note.getContent());
        return keywordExtractor.extractKeywords(text, maxKeywords);
    }

    public List<String> getRelatedKeywords(String keyword, int maxSuggestions)
            throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        if (maxSuggestions <= 0) {
            throw new IllegalArgumentException(
                    "maxSuggestions must be positive");
        }
        List<Note> allNotes = noteDAO.findAll();
        return keywordExtractor.suggestRelated(allNotes, keyword, maxSuggestions);
    }

    // ────────────────────── Validation ────────────────────

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Note title must not be blank");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Note title must not exceed " + MAX_TITLE_LENGTH
                            + " characters");
        }
    }

    private void validateId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private String combine(String title, String content) {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title);
        sb.append(' ');
        if (content != null) sb.append(content);
        return sb.toString();
    }
}
