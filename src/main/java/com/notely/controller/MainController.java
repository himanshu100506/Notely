package com.notely.controller;

import com.notely.model.Folder;
import com.notely.model.Note;
import com.notely.model.Tag;
import com.notely.service.FolderService;
import com.notely.service.NoteService;
import com.notely.service.TagService;
import com.notely.util.Icons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class MainController {

    private final NoteService noteService = new NoteService();
    private final FolderService folderService = new FolderService();
    private final TagService tagService = new TagService();

    private BorderPane root;
    private StackPane contentArea;
    private VBox folderList;
    private VBox tagList;
    private VBox navBox;

    private HomeController homeController;
    private AllPagesController allPagesController;
    private NoteEditorController noteEditorController;
    private SettingsController settingsController;

    private String activeNav = "home";

    public BorderPane getRoot() { return root; }
    public NoteService getNoteService() { return noteService; }
    public FolderService getFolderService() { return folderService; }
    public TagService getTagService() { return tagService; }

    public void initialize() {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        homeController = new HomeController(this);
        allPagesController = new AllPagesController(this);
        settingsController = new SettingsController(this);

        showHome();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setSpacing(0);

        Label brand = new Label("Notely");
        brand.getStyleClass().add("sidebar-brand");

        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.getStyleClass().add("sidebar-search");
        HBox searchBox = new HBox(searchField);
        searchBox.setPadding(new Insets(0, 12, 8, 12));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                showSearchResults(query);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                // If cleared, go back to current view
            }
        });

        navBox = new VBox(2);
        navBox.setPadding(new Insets(8, 0, 0, 0));
        navBox.getChildren().addAll(
                createNavItem("Home", Icons.HOME, "home"),
                createNavItem("All Pages", Icons.DOCUMENT, "allpages"),
                createNavItem("Favorites", Icons.STAR, "favorites"),
                createNavItem("Private Pages", Icons.STAR_OUTLINE, "private")
        );

        Label foldersHeader = new Label("FOLDERS");
        foldersHeader.getStyleClass().add("sidebar-section-header");

        HBox foldersHeaderRow = new HBox();
        foldersHeaderRow.setAlignment(Pos.CENTER_LEFT);
        foldersHeaderRow.getChildren().add(foldersHeader);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addFolderBtn = new Button("+");
        addFolderBtn.getStyleClass().addAll("button-ghost");
        addFolderBtn.setStyle("-fx-font-size: 14px; -fx-padding: 0 8;");
        addFolderBtn.setOnAction(e -> promptCreateFolder());
        foldersHeaderRow.getChildren().addAll(spacer, addFolderBtn);
        foldersHeaderRow.setPadding(new Insets(16, 12, 4, 16));

        folderList = new VBox(0);

        Label tagsHeader = new Label("TAGS");
        tagsHeader.getStyleClass().add("sidebar-section-header");

        HBox tagsHeaderRow = new HBox();
        tagsHeaderRow.setAlignment(Pos.CENTER_LEFT);
        tagsHeaderRow.getChildren().add(tagsHeader);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Button addTagBtn = new Button("+");
        addTagBtn.getStyleClass().addAll("button-ghost");
        addTagBtn.setStyle("-fx-font-size: 14px; -fx-padding: 0 8;");
        addTagBtn.setOnAction(e -> promptCreateTag());
        tagsHeaderRow.getChildren().addAll(spacer2, addTagBtn);
        tagsHeaderRow.setPadding(new Insets(16, 12, 4, 16));

        tagList = new VBox(0);

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        HBox settingsItem = createNavItem("Settings", Icons.SETTINGS, "settings");

        ScrollPane scrollContent = new ScrollPane();
        VBox scrollInner = new VBox();
        scrollInner.getChildren().addAll(
                navBox,
                foldersHeaderRow, folderList,
                tagsHeaderRow, tagList
        );
        scrollContent.setContent(scrollInner);
        scrollContent.setFitToWidth(true);
        scrollContent.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollContent.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollContent.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollContent, Priority.ALWAYS);

        sidebar.getChildren().addAll(brand, searchBox, scrollContent, settingsItem);

        refreshSidebar();
        return sidebar;
    }

    private HBox createNavItem(String text, String iconPath, String navId) {
        HBox item = new HBox(10);
        item.getStyleClass().add("nav-item");
        item.setAlignment(Pos.CENTER_LEFT);

        Region icon = Icons.icon(iconPath, 16);
        Label label = new Label(text);

        item.getChildren().addAll(icon, label);
        item.setOnMouseClicked(e -> navigateTo(navId));

        return item;
    }

    private void setActiveNavStyle(String navId) {
        activeNav = navId;
        for (Node child : navBox.getChildren()) {
            if (child instanceof HBox hbox) {
                hbox.getStyleClass().remove("nav-item-active");
            }
        }
        String[] navIds = {"home", "allpages", "favorites", "private"};
        for (int i = 0; i < navIds.length && i < navBox.getChildren().size(); i++) {
            if (navIds[i].equals(navId)) {
                navBox.getChildren().get(i).getStyleClass().add("nav-item-active");
            }
        }
    }

    public void navigateTo(String navId) {
        setActiveNavStyle(navId);
        switch (navId) {
            case "home" -> showHome();
            case "allpages" -> showAllPages();
            case "favorites" -> showFavorites();
            case "private" -> showAllPages();
            case "settings" -> showSettings();
        }
    }

    public void showHome() {
        setActiveNavStyle("home");
        contentArea.getChildren().setAll(homeController.getView());
        homeController.refresh();
    }

    public void showAllPages() {
        setActiveNavStyle("allpages");
        contentArea.getChildren().setAll(allPagesController.getView());
        allPagesController.refresh();
    }

    private void showFavorites() {
        setActiveNavStyle("favorites");
        contentArea.getChildren().setAll(allPagesController.getView());
        allPagesController.showPinned();
    }

    private void showSettings() {
        contentArea.getChildren().setAll(settingsController.getView());
    }

    public void showNoteEditor(Note note) {
        noteEditorController = new NoteEditorController(this, note);
        contentArea.getChildren().setAll(noteEditorController.getView());
    }

    public void showFolderNotes(Folder folder) {
        setActiveNavStyle("");
        contentArea.getChildren().setAll(allPagesController.getView());
        allPagesController.showFolderNotes(folder);
    }

    private void showSearchResults(String query) {
        contentArea.getChildren().setAll(allPagesController.getView());
        allPagesController.showSearchResults(query);
    }

    public void refreshSidebar() {
        Task<Void> task = new Task<>() {
            List<Folder> folders;
            List<Tag> tags;
            @Override
            protected Void call() throws SQLException {
                folders = folderService.getAllFolders();
                tags = tagService.getAllTags();
                return null;
            }
            @Override
            protected void succeeded() {
                folderList.getChildren().clear();
                for (Folder f : folders) {
                    HBox row = new HBox(8);
                    row.getStyleClass().add("nav-item");
                    row.setAlignment(Pos.CENTER_LEFT);
                    Region icon = Icons.icon(Icons.FOLDER, 14);
                    Label label = new Label(f.getName());
                    label.setStyle("-fx-font-size: 13px;");
                    row.getChildren().addAll(icon, label);
                    row.setOnMouseClicked(e -> showFolderNotes(f));

                    ContextMenu ctx = new ContextMenu();
                    MenuItem rename = new MenuItem("Rename");
                    rename.setOnAction(e -> promptRenameFolder(f));
                    MenuItem delete = new MenuItem("Delete");
                    delete.setOnAction(e -> confirmDeleteFolder(f));
                    ctx.getItems().addAll(rename, delete);
                    row.setOnContextMenuRequested(e ->
                            ctx.show(row, e.getScreenX(), e.getScreenY()));

                    folderList.getChildren().add(row);
                }

                tagList.getChildren().clear();
                FlowPane tagFlow = new FlowPane(6, 6);
                tagFlow.setPadding(new Insets(4, 12, 4, 16));
                for (Tag t : tags) {
                    Label pill = new Label(t.getName());
                    pill.getStyleClass().add("tag-pill");
                    tagFlow.getChildren().add(pill);
                }
                tagList.getChildren().add(tagFlow);
            }
        };
        new Thread(task).start();
    }

    private void promptCreateFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("Folder name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        folderService.createFolder(name);
                        return null;
                    }
                    @Override protected void succeeded() { refreshSidebar(); }
                    @Override protected void failed() { showError("Failed to create folder"); }
                };
                new Thread(task).start();
            }
        });
    }

    private void promptRenameFolder(Folder folder) {
        TextInputDialog dialog = new TextInputDialog(folder.getName());
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        folderService.renameFolder(folder.getId(), name);
                        return null;
                    }
                    @Override protected void succeeded() { refreshSidebar(); }
                    @Override protected void failed() { showError("Failed to rename folder"); }
                };
                new Thread(task).start();
            }
        });
    }

    private void confirmDeleteFolder(Folder folder) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete folder \"" + folder.getName() + "\"? Notes will be unfoldered, not deleted.",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        folderService.deleteFolder(folder.getId());
                        return null;
                    }
                    @Override protected void succeeded() {
                        refreshSidebar();
                        showHome();
                    }
                    @Override protected void failed() { showError("Failed to delete folder"); }
                };
                new Thread(task).start();
            }
        });
    }

    private void promptCreateTag() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Tag");
        dialog.setHeaderText(null);
        dialog.setContentText("Tag name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws SQLException {
                        tagService.createTag(name);
                        return null;
                    }
                    @Override protected void succeeded() { refreshSidebar(); }
                    @Override protected void failed() { showError("Failed to create tag"); }
                };
                new Thread(task).start();
            }
        });
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    public StackPane getContentArea() { return contentArea; }

    public void applyStartupTheme() {
        String theme = settingsController.getCurrentTheme();
        if ("dark".equals(theme)) {
            applyTheme("/com/notely/css/dark-theme.css");
        }
    }

    public void applyTheme(String themeCssPath) {
        root.getScene().getStylesheets().removeIf(s -> s.contains("theme"));
        if (themeCssPath != null) {
            String url = getClass().getResource(themeCssPath).toExternalForm();
            root.getScene().getStylesheets().add(url);
        }
    }
}
