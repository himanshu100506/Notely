package com.notely.controller;

import com.notely.model.Folder;
import com.notely.model.Note;
import com.notely.util.Icons;
import com.notely.util.TimeFormatter;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class HomeController {

    private final MainController main;
    private final VBox view;
    private final VBox pinnedBox;
    private final VBox recentBox;
    private final FlowPane folderBox;

    public HomeController(MainController main) {
        this.main = main;

        view = new VBox(20);
        view.setPadding(new Insets(32, 40, 32, 40));

        Label welcome = new Label("Welcome to Notely");
        welcome.getStyleClass().add("page-title");

        Button createBtn = new Button("+ Create new note");
        createBtn.getStyleClass().addAll("button", "button-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> createNewNote());

        Label pinnedHeader = new Label("Pinned Notes");
        pinnedHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        pinnedBox = new VBox(8);

        Label recentHeader = new Label("Recently Edited");
        recentHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        recentBox = new VBox(0);

        Label folderHeader = new Label("Folders");
        folderHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        folderBox = new FlowPane(12, 12);

        view.getChildren().addAll(
                welcome, createBtn,
                pinnedHeader, pinnedBox,
                recentHeader, recentBox,
                folderHeader, folderBox
        );
    }

    public VBox getView() { return view; }

    public void refresh() {
        Task<Void> task = new Task<>() {
            List<Note> pinned;
            List<Note> recent;
            List<Folder> folders;

            @Override
            protected Void call() throws SQLException {
                pinned = main.getNoteService().getPinnedNotes();
                recent = main.getNoteService().getRecentNotes(10);
                folders = main.getFolderService().getAllFolders();
                return null;
            }

            @Override
            protected void succeeded() {
                pinnedBox.getChildren().clear();
                if (pinned.isEmpty()) {
                    pinnedBox.getChildren().add(emptyLabel("No pinned notes"));
                } else {
                    FlowPane cards = new FlowPane(12, 12);
                    for (Note n : pinned) {
                        cards.getChildren().add(buildNoteCard(n));
                    }
                    pinnedBox.getChildren().add(cards);
                }

                recentBox.getChildren().clear();
                if (recent.isEmpty()) {
                    recentBox.getChildren().add(emptyLabel("No notes yet"));
                } else {
                    for (Note n : recent) {
                        recentBox.getChildren().add(buildNoteRow(n));
                    }
                }

                folderBox.getChildren().clear();
                for (Folder f : folders) {
                    folderBox.getChildren().add(buildFolderCard(f));
                }
            }

            @Override
            protected void failed() {
                main.showError("Failed to load home data");
            }
        };
        new Thread(task).start();
    }

    private void createNewNote() {
        Task<Note> task = new Task<>() {
            @Override
            protected Note call() throws SQLException {
                return main.getNoteService().createNote("Untitled", "");
            }

            @Override
            protected void succeeded() {
                main.showNoteEditor(getValue());
            }

            @Override
            protected void failed() {
                main.showError("Failed to create note");
            }
        };
        new Thread(task).start();
    }

    private VBox buildNoteCard(Note note) {
        VBox card = new VBox(4);
        card.getStyleClass().add("note-card");
        card.setPrefWidth(200);
        card.setMaxWidth(200);

        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        if (note.isPinned()) {
            Region pinIcon = Icons.icon(Icons.PIN, 12);
            pinIcon.getStyleClass().add("pin-icon");
            titleRow.getChildren().add(pinIcon);
        }
        Label title = new Label(note.getTitle());
        title.getStyleClass().add("note-card-title");
        title.setMaxWidth(170);
        titleRow.getChildren().add(title);

        String preview = note.getContent() != null ? note.getContent() : "";
        if (preview.startsWith("{")) preview = "";
        if (preview.length() > 60) preview = preview.substring(0, 60) + "...";
        Label previewLabel = new Label(preview.isEmpty() ? "Empty note" : preview);
        previewLabel.getStyleClass().add("note-card-preview");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(40);

        Label meta = new Label(TimeFormatter.formatRelative(note.getUpdatedAt()));
        meta.getStyleClass().add("note-card-meta");

        card.getChildren().addAll(titleRow, previewLabel, meta);
        card.setOnMouseClicked(e -> main.showNoteEditor(note));
        return card;
    }

    private HBox buildNoteRow(Note note) {
        HBox row = new HBox();
        row.getStyleClass().add("note-list-item");
        row.setAlignment(Pos.CENTER_LEFT);

        HBox left = new HBox(6);
        left.setAlignment(Pos.CENTER_LEFT);
        if (note.isPinned()) {
            Region pin = Icons.icon(Icons.PIN, 12);
            pin.getStyleClass().add("pin-icon");
            left.getChildren().add(pin);
        }
        Label title = new Label(note.getTitle());
        title.getStyleClass().add("note-list-title");
        left.getChildren().add(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(TimeFormatter.formatRelative(note.getUpdatedAt()));
        time.getStyleClass().add("note-list-meta");

        row.getChildren().addAll(left, spacer, time);
        row.setOnMouseClicked(e -> main.showNoteEditor(note));
        return row;
    }

    private VBox buildFolderCard(Folder folder) {
        VBox card = new VBox(4);
        card.getStyleClass().add("folder-card");
        card.setPrefWidth(140);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        Region icon = Icons.icon(Icons.FOLDER, 14);
        Label name = new Label(folder.getName());
        name.getStyleClass().add("folder-card-name");
        header.getChildren().addAll(icon, name);

        card.getChildren().add(header);
        card.setOnMouseClicked(e -> main.showFolderNotes(folder));
        return card;
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: -notely-text-tertiary; -fx-font-size: 13px;");
        return label;
    }
}
