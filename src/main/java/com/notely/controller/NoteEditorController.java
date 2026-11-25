package com.notely.controller;

import com.notely.model.Folder;
import com.notely.model.Note;
import com.notely.model.Tag;
import com.notely.util.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoteEditorController {

    private final MainController main;
    private Note note;
    private final BorderPane view;
    private final TextField titleField;
    private final VBox blocksContainer;
    private final Label statusLabel;
    private final FlowPane tagFlow;

    private final List<BlockData> blocks = new ArrayList<>();
    private Timer saveTimer;
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private boolean dirty = false;
    private String lastSavedContent = "";
    private String lastSavedTitle = "";

    public NoteEditorController(MainController main, Note note) {
        this.main = main;
        this.note = note;

        view = new BorderPane();

        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("editor-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().addAll("button", "button-ghost");
        backBtn.setOnAction(e -> goBack());

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);

        Button pinBtn = new Button(note.isPinned() ? "Unpin" : "Pin");
        pinBtn.getStyleClass().addAll("button", "button-ghost");
        pinBtn.setOnAction(e -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws SQLException {
                    Note updated = main.getNoteService().togglePin(note.getId());
                    note.setPinned(updated.isPinned());
                    return null;
                }

                @Override
                protected void succeeded() {
                    pinBtn.setText(note.isPinned() ? "Unpin" : "Pin");
                }
            };
            new Thread(task).start();
        });

        MenuButton folderMenu = new MenuButton("Folder");
        folderMenu.getStyleClass().addAll("button", "button-ghost");
        refreshFolderMenu(folderMenu);

        toolbar.getChildren().addAll(backBtn, tbSpacer, pinBtn, folderMenu);
        view.setTop(toolbar);

        VBox editorArea = new VBox(0);
        editorArea.getStyleClass().add("editor-area");

        titleField = new TextField(note.getTitle());
        titleField.getStyleClass().add("editor-title");
        titleField.setPromptText("Untitled");
        titleField.textProperty().addListener((obs, o, n) -> markDirty());

        HBox tagBar = new HBox(8);
        tagBar.setAlignment(Pos.CENTER_LEFT);
        tagBar.setPadding(new Insets(0, 0, 12, 0));

        Label tagLabel = new Label("Tags:");
        tagLabel.setStyle("-fx-text-fill: -notely-text-secondary; -fx-font-size: 12px;");
        tagFlow = new FlowPane(4, 4);
        Button addTagBtn = new Button("+");
        addTagBtn.getStyleClass().add("button-ghost");
        addTagBtn.setStyle("-fx-font-size: 12px; -fx-padding: 0 6;");
        addTagBtn.setOnAction(e -> showTagPicker());
        tagBar.getChildren().addAll(tagLabel, tagFlow, addTagBtn);

        blocksContainer = new VBox(2);
        blocksContainer.setPadding(new Insets(8, 0, 0, 0));

        VBox scrollInner = new VBox(titleField, tagBar, blocksContainer);
        scrollInner.setPadding(new Insets(32, 48, 32, 48));
        ScrollPane scroll = new ScrollPane(scrollInner);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        editorArea.getChildren().add(scroll);
        view.setCenter(editorArea);

        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("editor-status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 16, 4, 16));
        statusLabel = new Label("Saved");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -notely-text-tertiary;");
        statusBar.getChildren().add(statusLabel);
        view.setBottom(statusBar);

        loadContent();
        loadTags();

        lastSavedTitle = note.getTitle() != null ? note.getTitle() : "";
        lastSavedContent = note.getContent() != null ? note.getContent() : "";
    }

    public BorderPane getView() {
        return view;
    }

    private void loadContent() {
        blocks.clear();
        DocumentData doc = BlockSerializer.deserialize(note.getContent());
        if (doc.getBlocks().isEmpty()) {
            blocks.add(BlockData.paragraph(""));
        } else {
            blocks.addAll(doc.getBlocks());
        }
        renderBlocks();
    }

    private void renderBlocks() {
        blocksContainer.getChildren().clear();
        for (int i = 0; i < blocks.size(); i++) {
            blocksContainer.getChildren().add(buildBlockNode(i));
        }
    }

    private Node buildBlockNode(int index) {
        BlockData block = blocks.get(index);
        return switch (block.getType()) {
            case PARAGRAPH -> buildTextBlock(index, "block-paragraph", "Type here...");
            case H1 -> buildTextBlock(index, "block-h1", "Heading 1");
            case H2 -> buildTextBlock(index, "block-h2", "Heading 2");
            case H3 -> buildTextBlock(index, "block-h3", "Heading 3");
            case CODE -> buildCodeBlock(index);
            case QUOTE -> buildTextBlock(index, "block-quote", "Quote...");
            case BULLET -> buildListBlock(index, "• ");
            case NUMBERED -> buildListBlock(index, "");
            case CHECKLIST -> buildChecklistBlock(index);
            case DIVIDER -> buildDividerBlock(index);
        };
    }

    private Node buildTextBlock(int index, String styleClass, String prompt) {
        BlockData block = blocks.get(index);
        TextArea area = new TextArea(block.getText() != null ? block.getText() : "");
        area.setPromptText(prompt);
        area.getStyleClass().addAll("block", styleClass);
        area.setWrapText(true);
        area.setPrefRowCount(1);

        autoResizeTextArea(area);
        area.textProperty().addListener((obs, o, n) -> {
            blocks.set(index, createBlockFromType(block.getType(), n));
            markDirty();
            autoResizeTextArea(area);
        });

        setupBlockKeyHandlers(area, index);
        return wrapBlock(area, index);
    }

    private Node buildCodeBlock(int index) {
        BlockData block = blocks.get(index);
        TextArea area = new TextArea(block.getText() != null ? block.getText() : "");
        area.setPromptText("Code...");
        area.getStyleClass().addAll("block", "block-code");
        area.setWrapText(false);
        area.setPrefRowCount(3);

        area.textProperty().addListener((obs, o, n) -> {
            blocks.set(index, BlockData.code(n));
            markDirty();
        });

        setupBlockKeyHandlers(area, index);
        return wrapBlock(area, index);
    }

    private Node buildListBlock(int index, String prefix) {
        BlockData block = blocks.get(index);
        VBox listBox = new VBox(2);
        listBox.getStyleClass().add("block");

        List<String> items = block.getItems() != null ? new ArrayList<>(block.getItems()) : new ArrayList<>();
        if (items.isEmpty()) items.add("");

        for (int i = 0; i < items.size(); i++) {
            final int itemIdx = i;
            HBox row = new HBox(4);
            row.setAlignment(Pos.CENTER_LEFT);

            String bullet = block.getType() == BlockType.NUMBERED
                    ? (itemIdx + 1) + ". " : prefix;
            Label bulletLabel = new Label(bullet);
            bulletLabel.setStyle("-fx-text-fill: -notely-text-secondary; -fx-font-size: 14px; -fx-min-width: 20;");

            TextField tf = new TextField(items.get(i));
            tf.getStyleClass().add("block-list-field");
            HBox.setHgrow(tf, Priority.ALWAYS);

            tf.textProperty().addListener((obs, o, n) -> {
                items.set(itemIdx, n);
                if (block.getType() == BlockType.BULLET) {
                    blocks.set(index, BlockData.bullet(items));
                } else {
                    blocks.set(index, BlockData.numbered(items));
                }
                markDirty();
            });

            tf.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    if (tf.getText().isEmpty() && items.size() > 1) {
                        items.remove(itemIdx);
                        if (block.getType() == BlockType.BULLET) {
                            blocks.set(index, BlockData.bullet(items));
                        } else {
                            blocks.set(index, BlockData.numbered(items));
                        }
                        addBlockAfter(index, BlockData.paragraph(""));
                    } else {
                        items.add(itemIdx + 1, "");
                        if (block.getType() == BlockType.BULLET) {
                            blocks.set(index, BlockData.bullet(items));
                        } else {
                            blocks.set(index, BlockData.numbered(items));
                        }
                        markDirty();
                        renderBlocks();
                        focusListItem(index, itemIdx + 1);
                    }
                } else if (e.getCode() == KeyCode.BACK_SPACE && tf.getText().isEmpty() && items.size() > 1) {
                    e.consume();
                    items.remove(itemIdx);
                    if (block.getType() == BlockType.BULLET) {
                        blocks.set(index, BlockData.bullet(items));
                    } else {
                        blocks.set(index, BlockData.numbered(items));
                    }
                    markDirty();
                    renderBlocks();
                    focusListItem(index, Math.max(0, itemIdx - 1));
                }
            });

            row.getChildren().addAll(bulletLabel, tf);
            listBox.getChildren().add(row);
        }
        return wrapBlock(listBox, index);
    }

    private Node buildChecklistBlock(int index) {
        BlockData block = blocks.get(index);
        VBox listBox = new VBox(2);
        listBox.getStyleClass().add("block");

        List<BlockData.ChecklistItem> items = block.getChecklist() != null
                ? new ArrayList<>(block.getChecklist()) : new ArrayList<>();
        if (items.isEmpty()) items.add(new BlockData.ChecklistItem("", false));

        for (int i = 0; i < items.size(); i++) {
            final int itemIdx = i;
            HBox row = new HBox(4);
            row.setAlignment(Pos.CENTER_LEFT);

            CheckBox cb = new CheckBox();
            cb.setSelected(items.get(i).isChecked());

            TextField tf = new TextField(items.get(i).getText());
            tf.getStyleClass().add("block-list-field");
            HBox.setHgrow(tf, Priority.ALWAYS);

            cb.selectedProperty().addListener((obs, o, n) -> {
                items.set(itemIdx, new BlockData.ChecklistItem(tf.getText(), n));
                blocks.set(index, BlockData.checklist(items));
                markDirty();
            });

            tf.textProperty().addListener((obs, o, n) -> {
                items.set(itemIdx, new BlockData.ChecklistItem(n, cb.isSelected()));
                blocks.set(index, BlockData.checklist(items));
                markDirty();
            });

            tf.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    items.add(itemIdx + 1, new BlockData.ChecklistItem("", false));
                    blocks.set(index, BlockData.checklist(items));
                    markDirty();
                    renderBlocks();
                    focusListItem(index, itemIdx + 1);
                } else if (e.getCode() == KeyCode.BACK_SPACE && tf.getText().isEmpty() && items.size() > 1) {
                    e.consume();
                    items.remove(itemIdx);
                    blocks.set(index, BlockData.checklist(items));
                    markDirty();
                    renderBlocks();
                    focusListItem(index, Math.max(0, itemIdx - 1));
                }
            });

            row.getChildren().addAll(cb, tf);
            listBox.getChildren().add(row);
        }
        return wrapBlock(listBox, index);
    }

    private Node buildDividerBlock(int index) {
        HBox wrapper = new HBox();
        wrapper.getStyleClass().add("block");
        wrapper.setPadding(new Insets(8, 0, 8, 0));
        Separator sep = new Separator();
        sep.getStyleClass().add("block-divider");
        HBox.setHgrow(sep, Priority.ALWAYS);
        wrapper.getChildren().add(sep);
        wrapper.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                blocks.remove(index);
                markDirty();
                renderBlocks();
            }
        });
        return wrapper;
    }

    private HBox wrapBlock(Node blockNode, int index) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.TOP_LEFT);

        MenuButton blockMenu = new MenuButton("⋮");
        blockMenu.getStyleClass().add("button-ghost");
        blockMenu.setStyle("-fx-font-size: 14px; -fx-padding: 2 4; -fx-opacity: 0.3;");
        blockMenu.setOnMouseEntered(e -> blockMenu.setStyle("-fx-font-size: 14px; -fx-padding: 2 4; -fx-opacity: 1;"));
        blockMenu.setOnMouseExited(e -> blockMenu.setStyle("-fx-font-size: 14px; -fx-padding: 2 4; -fx-opacity: 0.3;"));

        MenuItem deleteBlock = new MenuItem("Delete block");
        deleteBlock.setOnAction(e -> {
            if (blocks.size() > 1) {
                blocks.remove(index);
                markDirty();
                renderBlocks();
            }
        });

        MenuItem addAbove = new MenuItem("Add block above");
        addAbove.setOnAction(e -> {
            blocks.add(index, BlockData.paragraph(""));
            markDirty();
            renderBlocks();
            focusBlock(index);
        });

        MenuItem addBelow = new MenuItem("Add block below");
        addBelow.setOnAction(e -> addBlockAfter(index, BlockData.paragraph("")));

        blockMenu.getItems().addAll(addAbove, addBelow, new SeparatorMenuItem(), deleteBlock);

        HBox.setHgrow(blockNode, Priority.ALWAYS);
        if (blockNode instanceof VBox vbox) {
            HBox.setHgrow(vbox, Priority.ALWAYS);
        }
        wrapper.getChildren().addAll(blockMenu, blockNode);
        return wrapper;
    }

    private void setupBlockKeyHandlers(TextArea area, int index) {
        area.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                if (area.getText().isEmpty() || area.getCaretPosition() == area.getText().length()) {
                    e.consume();
                    addBlockAfter(index, BlockData.paragraph(""));
                }
            } else if (e.getCode() == KeyCode.BACK_SPACE && area.getText().isEmpty() && blocks.size() > 1) {
                e.consume();
                blocks.remove(index);
                markDirty();
                renderBlocks();
                focusBlock(Math.max(0, index - 1));
            } else if (e.getCode() == KeyCode.SLASH && area.getText().isEmpty()) {
                Platform.runLater(() -> showSlashMenu(area, index));
            } else if (e.getCode() == KeyCode.UP && area.getCaretPosition() == 0 && index > 0) {
                e.consume();
                focusBlock(index - 1);
            } else if (e.getCode() == KeyCode.DOWN
                    && area.getCaretPosition() == area.getText().length()
                    && index < blocks.size() - 1) {
                e.consume();
                focusBlock(index + 1);
            }
        });
    }

    private void showSlashMenu(TextArea area, int index) {
        ContextMenu menu = new ContextMenu();

        String[][] types = {
                {"Paragraph", "paragraph"}, {"Heading 1", "h1"}, {"Heading 2", "h2"},
                {"Heading 3", "h3"}, {"Bullet List", "bullet"}, {"Numbered List", "numbered"},
                {"Checklist", "checklist"}, {"Code Block", "code"}, {"Quote", "quote"},
                {"Divider", "divider"}
        };

        for (String[] t : types) {
            MenuItem item = new MenuItem(t[0]);
            final String typeStr = t[1];
            item.setOnAction(e -> {
                area.setText("");
                convertBlock(index, typeStr);
            });
            menu.getItems().add(item);
        }

        menu.show(area, Side.BOTTOM, 0, 0);
    }

    private void convertBlock(int index, String typeStr) {
        BlockData newBlock = switch (typeStr) {
            case "h1" -> BlockData.h1("");
            case "h2" -> BlockData.h2("");
            case "h3" -> BlockData.h3("");
            case "bullet" -> BlockData.bullet(List.of(""));
            case "numbered" -> BlockData.numbered(List.of(""));
            case "checklist" -> BlockData.checklist(List.of(new BlockData.ChecklistItem("", false)));
            case "code" -> BlockData.code("");
            case "quote" -> BlockData.quote("");
            case "divider" -> BlockData.divider();
            default -> BlockData.paragraph("");
        };
        blocks.set(index, newBlock);
        markDirty();
        renderBlocks();
        focusBlock(index);
    }

    private void addBlockAfter(int index, BlockData block) {
        blocks.add(index + 1, block);
        markDirty();
        renderBlocks();
        focusBlock(index + 1);
    }

    private void focusBlock(int index) {
        Platform.runLater(() -> {
            if (index < 0 || index >= blocksContainer.getChildren().size()) return;
            Node wrapper = blocksContainer.getChildren().get(index);
            if (wrapper instanceof HBox hbox && hbox.getChildren().size() > 1) {
                Node blockNode = hbox.getChildren().get(1);
                if (blockNode instanceof TextArea ta) {
                    ta.requestFocus();
                    ta.positionCaret(ta.getText().length());
                } else if (blockNode instanceof VBox vbox && !vbox.getChildren().isEmpty()) {
                    focusLastFieldInVBox(vbox);
                }
            }
        });
    }

    private void focusListItem(int blockIndex, int itemIndex) {
        Platform.runLater(() -> {
            if (blockIndex < 0 || blockIndex >= blocksContainer.getChildren().size()) return;
            Node wrapper = blocksContainer.getChildren().get(blockIndex);
            if (wrapper instanceof HBox hbox && hbox.getChildren().size() > 1) {
                Node blockNode = hbox.getChildren().get(1);
                if (blockNode instanceof VBox vbox && itemIndex < vbox.getChildren().size()) {
                    Node row = vbox.getChildren().get(itemIndex);
                    if (row instanceof HBox rowBox) {
                        for (Node child : rowBox.getChildren()) {
                            if (child instanceof TextField tf) {
                                tf.requestFocus();
                                tf.positionCaret(tf.getText().length());
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    private void focusLastFieldInVBox(VBox vbox) {
        if (vbox.getChildren().isEmpty()) return;
        Node lastRow = vbox.getChildren().getLast();
        if (lastRow instanceof HBox hbox) {
            for (Node child : hbox.getChildren()) {
                if (child instanceof TextField tf) {
                    tf.requestFocus();
                    tf.positionCaret(tf.getText().length());
                    return;
                }
            }
        }
    }

    private BlockData createBlockFromType(BlockType type, String text) {
        return switch (type) {
            case PARAGRAPH -> BlockData.paragraph(text);
            case H1 -> BlockData.h1(text);
            case H2 -> BlockData.h2(text);
            case H3 -> BlockData.h3(text);
            case CODE -> BlockData.code(text);
            case QUOTE -> BlockData.quote(text);
            default -> BlockData.paragraph(text);
        };
    }

    private void autoResizeTextArea(TextArea area) {
        Platform.runLater(() -> {
            area.setMinHeight(Region.USE_PREF_SIZE);
            int lines = area.getText().split("\n", -1).length;
            double lineHeight = 22;
            if (area.getStyleClass().contains("block-h1")) lineHeight = 34;
            else if (area.getStyleClass().contains("block-h2")) lineHeight = 28;
            else if (area.getStyleClass().contains("block-h3")) lineHeight = 24;
            area.setPrefHeight(Math.max(lineHeight + 10, lines * lineHeight + 10));
        });
    }

    // ──────────────── Auto-save ──────────────────────

    private void markDirty() {
        dirty = true;
        statusLabel.setText("Editing...");
        scheduleSave();
    }

    private void scheduleSave() {
        if (saveTimer != null) {
            saveTimer.cancel();
        }
        saveTimer = new Timer(true);
        saveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> saveNow());
            }
        }, 1500);
    }

    private void saveNow() {
        if (!dirty || saving.get()) return;

        String currentTitle = titleField.getText().trim();
        if (currentTitle.isEmpty()) currentTitle = "Untitled";

        DocumentData doc = new DocumentData(new ArrayList<>(blocks));
        String currentContent = BlockSerializer.serialize(doc);

        if (currentTitle.equals(lastSavedTitle) && currentContent.equals(lastSavedContent)) {
            dirty = false;
            statusLabel.setText("Saved");
            return;
        }

        saving.set(true);
        statusLabel.setText("Saving...");

        final String title = currentTitle;
        final String content = currentContent;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                note.setTitle(title);
                note.setContent(content);
                main.getNoteService().updateNote(note);
                return null;
            }

            @Override
            protected void succeeded() {
                lastSavedTitle = title;
                lastSavedContent = content;
                dirty = false;
                saving.set(false);
                statusLabel.setText("Saved");
            }

            @Override
            protected void failed() {
                saving.set(false);
                statusLabel.setText("Save failed!");
            }
        };
        new Thread(task).start();
    }

    private void goBack() {
        if (dirty) saveNow();
        if (saveTimer != null) saveTimer.cancel();
        main.refreshSidebar();
        main.showHome();
    }

    // ──────────────── Tags ───────────────────────────

    private void loadTags() {
        Task<List<Tag>> task = new Task<>() {
            @Override
            protected List<Tag> call() throws SQLException {
                return main.getTagService().getTagsForNote(note.getId());
            }

            @Override
            protected void succeeded() {
                renderTags(getValue());
            }
        };
        new Thread(task).start();
    }

    private void renderTags(List<Tag> tags) {
        tagFlow.getChildren().clear();
        for (Tag t : tags) {
            HBox pill = new HBox(4);
            pill.getStyleClass().add("tag-pill");
            pill.setAlignment(Pos.CENTER);
            Label name = new Label(t.getName());
            name.setStyle("-fx-font-size: 12px;");
            Button removeBtn = new Button("×");
            removeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 0 2; -fx-background-color: transparent; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> removeTag(t));
            pill.getChildren().addAll(name, removeBtn);
            tagFlow.getChildren().add(pill);
        }
    }

    private void showTagPicker() {
        Task<List<Tag>> task = new Task<>() {
            @Override
            protected List<Tag> call() throws SQLException {
                return main.getTagService().getAllTags();
            }

            @Override
            protected void succeeded() {
                List<Tag> allTags = getValue();
                if (allTags.isEmpty()) {
                    promptCreateAndAssignTag();
                    return;
                }
                ChoiceDialog<String> dialog = new ChoiceDialog<>(
                        allTags.getFirst().getName(),
                        allTags.stream().map(Tag::getName).toList());
                dialog.setTitle("Add Tag");
                dialog.setHeaderText(null);
                dialog.setContentText("Select a tag:");
                dialog.showAndWait().ifPresent(tagName ->
                        allTags.stream()
                                .filter(t -> t.getName().equals(tagName))
                                .findFirst()
                                .ifPresent(this::assignTag));
            }

            private void assignTag(Tag tag) {
                Task<Void> inner = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        main.getTagService().addTagToNote(note.getId(), tag.getId());
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        loadTags();
                    }

                    @Override
                    protected void failed() {
                        loadTags();
                    }
                };
                new Thread(inner).start();
            }
        };
        new Thread(task).start();
    }

    private void promptCreateAndAssignTag() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Tag");
        dialog.setHeaderText("No tags exist yet. Create one:");
        dialog.setContentText("Tag name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws SQLException {
                        Tag tag = main.getTagService().createTag(name);
                        main.getTagService().addTagToNote(note.getId(), tag.getId());
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        loadTags();
                        main.refreshSidebar();
                    }
                };
                new Thread(task).start();
            }
        });
    }

    private void removeTag(Tag tag) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                main.getTagService().removeTagFromNote(note.getId(), tag.getId());
                return null;
            }

            @Override
            protected void succeeded() {
                loadTags();
            }
        };
        new Thread(task).start();
    }

    // ──────────────── Folder Menu ────────────────────

    private void refreshFolderMenu(MenuButton menu) {
        Task<List<Folder>> task = new Task<>() {
            @Override
            protected List<Folder> call() throws SQLException {
                return main.getFolderService().getAllFolders();
            }

            @Override
            protected void succeeded() {
                menu.getItems().clear();
                MenuItem noFolder = new MenuItem("No Folder");
                noFolder.setOnAction(e -> moveToFolder(null, menu));
                menu.getItems().add(noFolder);

                for (Folder f : getValue()) {
                    MenuItem item = new MenuItem(f.getName());
                    item.setOnAction(e -> moveToFolder(f.getId(), menu));
                    menu.getItems().add(item);
                }

                if (note.getFolderId() != null) {
                    getValue().stream()
                            .filter(f -> f.getId() == note.getFolderId())
                            .findFirst()
                            .ifPresent(f -> menu.setText(f.getName()));
                } else {
                    menu.setText("No Folder");
                }
            }
        };
        new Thread(task).start();
    }

    private void moveToFolder(Integer folderId, MenuButton menu) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws SQLException {
                main.getNoteService().moveToFolder(note.getId(), folderId);
                note.setFolderId(folderId);
                return null;
            }

            @Override
            protected void succeeded() {
                refreshFolderMenu(menu);
                main.refreshSidebar();
            }
        };
        new Thread(task).start();
    }
}
