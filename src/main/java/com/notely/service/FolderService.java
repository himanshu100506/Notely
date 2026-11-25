package com.notely.service;

import com.notely.dao.FolderDAO;
import com.notely.model.Folder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for folder operations.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Input validation (name not blank, within length limit).</li>
 *   <li>Delegating persistence to {@link FolderDAO}.</li>
 *   <li>Consistent handling of missing records.</li>
 * </ul>
 *
 * <h3>Dependency injection</h3>
 * The DAO is accepted via the constructor. A no-arg constructor
 * provides the default production wiring.
 */
public class FolderService {

    private static final int MAX_FOLDER_NAME_LENGTH = 100;

    private final FolderDAO folderDAO;

    public FolderService() {
        this(new FolderDAO());
    }

    public FolderService(FolderDAO folderDAO) {
        this.folderDAO = folderDAO;
    }

    // ────────────────────── Commands ──────────────────────

    public Folder createFolder(String name) throws SQLException {
        validateFolderName(name);
        Folder folder = new Folder(name.trim());
        folderDAO.createFolder(folder);
        return folder;
    }

    public Folder renameFolder(int folderId, String newName) throws SQLException {
        validateId(folderId, "Folder ID");
        validateFolderName(newName);
        Folder folder = folderDAO.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Folder not found: " + folderId));
        folder.setName(newName.trim());
        folderDAO.updateFolder(folder);
        return folder;
    }

    public void deleteFolder(int folderId) throws SQLException {
        validateId(folderId, "Folder ID");
        folderDAO.deleteFolder(folderId);
    }

    // ────────────────────── Queries ───────────────────────

    public Optional<Folder> getFolderById(int id) throws SQLException {
        validateId(id, "Folder ID");
        return folderDAO.findById(id);
    }

    public List<Folder> getAllFolders() throws SQLException {
        return folderDAO.findAll();
    }

    // ────────────────────── Validation ────────────────────

    private void validateFolderName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Folder name must not be blank");
        }
        if (name.trim().length() > MAX_FOLDER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Folder name must not exceed " + MAX_FOLDER_NAME_LENGTH
                            + " characters");
        }
    }

    private void validateId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }
}
