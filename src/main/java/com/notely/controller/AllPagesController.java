package com.notely.controller;

import com.notely.model.Folder;
import com.notely.model.Note;
import com.notely.util.Icons;
import com.notely.util.TimeFormatter;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class AllPagesController {

    private final MainController main;
    private final VBox view;
    private final TextField filterField;
    private final VBox noteListBox;
    private final Label titleLabel;
    private final HBox sortBar;

    private String currentSort = "date";
    private String currentFolder = null;
    private int currentFolderId = -1;

    public AllPagesController(MainController main) {
        this.main = main;

        view = new VBox(12);
        view.setPadding(new Insets(24, 32, 24, 32));

        titleLabel = new Label("All Pages");
        titleLabel.getStyleClass().add("page-title");

        filterField = new TextField();
        filterField.setPromptText("Filter notes...");
        filterField.getStyleClass().add("text-field");
        filterField.setOnAction(e -> applyFilter());
        filterField.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.isEmpty()) loadNotes();
        });

        sortBar = new HBox(12);
        sortBar.setAlignment(Pos.CENTER_LEFT);

        Button sortTitle = new Button("Sort by Title");
        sortTitle.getStyleClass().addAll("button", "button-ghost");
        sortTitle.setOnAction(e -> { currentSort = "title"; loadNotes(); });

        Button sortDate = new Button("Sort by Date");
        sortDate.getStyleClass().addAll("button", "button-ghost");
        sortDate.setOnAction(e -> { currentSort = "date"; loadNotes(); });

        Button newNoteBtn = new Button("+ New Note");
        newNoteBtn.getStyleClass().addAll("button", "button-primary");
        newNoteBtn.setOnAction(e -> createNote());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        sortBar.getChildren().addAll(sortTitle, sortDate, spacer, newNoteBtn);

        noteListBox = new VBox(0);

        ScrollPane scroll = new ScrollPane(noteListBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(titleLabel, filterField, sortBar, scroll);
    }

    public VBox getView() { return view; }

    public void refresh() {
        titleLabel.setText("All Pages");
        currentFolder = null;
        currentFolderId = -1;
        filterField.clear();
        currentSort = "date";
        loadNotes();
    }

    public void showPinned() {
        titleLabel.setText("Favorites");
        currentFolder = null;
        currentFolderId = -1;
        filterField.clear();
        loadPinnedNotes();
    }

    public void showFolderNotes(Folder folder) {
        titleLabel.setText(folder.getName());
        currentFolder = folder.getName();
        currentFolderId = folder.getId();
        filterField.clear();
        loadFolderNotes(folder.getId());
    }

    public void showSearchResults(String query) {
        titleLabel.setText("Search: " + query);
        filterField.setText(query);
        applyFilter();
    }

    private void applyFilter() {
        String query = filterField.getText().trim();
        if (query.isEmpty()) {
            loadNotes();
            return;
        }
        Task<List<Note>> task = new Task<>() {
            @Override
            protected List<Note> call() throws SQLException {
                return main.getNoteService().searchNotes(query);
            }

            @Override
            protected void succeeded() { renderNotes(getValue()); }

            @Override
            protected void failed() { main.showError("Search failed"); }
        };
        new Thread(task).start();
    }

    private void loadNotes() {
        Task<List<Note>> task = new Task<>() {
            @Override
            protected List<Note> call() throws SQLException {
                return currentSort.equals("title")
                        ? main.getNoteService().getNotesSortedByTitle()
                        : main.getNoteService().getNotesSortedByDate();
            }

            @Override
            protected void succeeded() { renderNotes(getValue()); }

            @Override
            protected void failed() { main.showError("Failed to load notes"); }
        };
        new Thread(task).start();
    }

    private void loadPinnedNotes() {
        Task<List<Note>> task = new Task<>() {
            @Override
            protected List<Note> call() throws SQLException {
                return main.getNoteService().getPinnedNotes();
            }

            @Override
            protected void succeeded() { renderNotes(getValue()); }

            @Override
            protected void failed() { main.showError("Failed to load pinned notes"); }
        };
        new Thread(task).start();
    }

    private void loadFolderNotes(int folderId) {
        Task<List<Note>> task = new Task<>() {
            @Override
            protected List<Note> call() throws SQLException {
                return main.getNoteService().getNotesByFolder(folderId);
            }

            @Override
            protected void succeeded() { renderNotes(getValue()); }

            @Override
            protected void failed() { main.showError("Failed to load folder notes"); }
        };
        new Thread(task).start();
    }

    private void renderNotes(List<Note> notes) {
        noteListBox.getChildren().clear();
        if (notes.isEmpty()) {
            Label empty = new Label("No notes found");
            empty.setStyle("-fx-text-fill: -notely-text-tertiary; -fx-padding: 20;");
            noteListBox.getChildren().add(empty);
            return;
        }
        for (Note note : notes) {
            noteListBox.getChildren().add(buildNoteRow(note));
        }
    }

    private HBox buildNoteRow(Note note) {
        HBox row = new HBox(12);
        row.getStyleClass().add("note-list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textCol = new VBox(2);
        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        if (note.isPinned()) {
            Region pin = Icons.icon(Icons.PIN, 12);
            pin.getStyleClass().add("pin-icon");
            titleRow.getChildren().add(pin);
        }
        Label title = new Label(note.getTitle());
        title.getStyleClass().add("note-list-title");
        titleRow.getChildren().add(title);

        Label meta = new Label(TimeFormatter.formatRelative(note.getUpdatedAt()));
        meta.getStyleClass().add("note-list-meta");

        textCol.getChildren().addAll(titleRow, meta);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        row.getChildren().add(textCol);
        row.setOnMouseClicked(e -> main.showNoteEditor(note));

        ContextMenu ctx = new ContextMenu();
        MenuItem pinItem = new MenuItem(note.isPinned() ? "Unpin" : "Pin");
        pinItem.setOnAction(e -> togglePin(note));
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> confirmDelete(note));
        ctx.getItems().addAll(pinItem, deleteItem);
        row.setOnContextMenuRequested(e -> ctx.show(row, e.getScreenX(), e.getScreenY()));

        return row;
    }

    private void togglePin(Note note) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                main.getNoteService().togglePin(note.getId());
                return null;
            }

            @Override
            protected void succeeded() { loadNotes(); }
        };
        new Thread(task).start();
    }

    private void confirmDelete(Note note) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + note.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        main.getNoteService().deleteNote(note.getId());
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        loadNotes();
                        main.refreshSidebar();
                    }
                };
                new Thread(task).start();
            }
        });
    }

    private void createNote() {
        Task<Note> task = new Task<>() {
            @Override
            protected Note call() throws SQLException {
                Note n = main.getNoteService().createNote("Untitled", "");
                if (currentFolderId > 0) {
                    main.getNoteService().moveToFolder(n.getId(), currentFolderId);
                }
                return n;
            }

            @Override
            protected void succeeded() {
                main.showNoteEditor(getValue());
            }
        };
        new Thread(task).start();
    }
}
