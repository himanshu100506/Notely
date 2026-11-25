package com.notely.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single block in a Notely document.
 *
 * <p>Different block types use different fields:</p>
 * <ul>
 *   <li>Paragraph, H1–H3, Code, Quote → {@link #getText()}</li>
 *   <li>Bullet, Numbered → {@link #getItems()}</li>
 *   <li>Checklist → {@link #getChecklist()}</li>
 *   <li>Divider → no content fields</li>
 * </ul>
 *
 * <p>Unused fields are left {@code null} and excluded from JSON
 * by Gson's default behavior.</p>
 */
public class BlockData {

    private BlockType type;
    private String text;
    private List<String> items;
    private List<ChecklistItem> checklist;

    public BlockData() {}

    private BlockData(BlockType type) {
        this.type = type;
    }

    // ──────────────── Factory Methods ────────────────────────

    public static BlockData paragraph(String text) {
        BlockData block = new BlockData(BlockType.PARAGRAPH);
        block.text = text;
        return block;
    }

    public static BlockData h1(String text) {
        BlockData block = new BlockData(BlockType.H1);
        block.text = text;
        return block;
    }

    public static BlockData h2(String text) {
        BlockData block = new BlockData(BlockType.H2);
        block.text = text;
        return block;
    }

    public static BlockData h3(String text) {
        BlockData block = new BlockData(BlockType.H3);
        block.text = text;
        return block;
    }

    public static BlockData bullet(List<String> items) {
        BlockData block = new BlockData(BlockType.BULLET);
        block.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        return block;
    }

    public static BlockData numbered(List<String> items) {
        BlockData block = new BlockData(BlockType.NUMBERED);
        block.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        return block;
    }

    public static BlockData checklist(List<ChecklistItem> items) {
        BlockData block = new BlockData(BlockType.CHECKLIST);
        block.checklist = items != null ? new ArrayList<>(items) : new ArrayList<>();
        return block;
    }

    public static BlockData code(String text) {
        BlockData block = new BlockData(BlockType.CODE);
        block.text = text;
        return block;
    }

    public static BlockData quote(String text) {
        BlockData block = new BlockData(BlockType.QUOTE);
        block.text = text;
        return block;
    }

    public static BlockData divider() {
        return new BlockData(BlockType.DIVIDER);
    }

    // ──────────────── Getters ────────────────────────────────

    public BlockType getType() { return type; }

    public String getText() { return text; }

    public List<String> getItems() { return items; }

    public List<ChecklistItem> getChecklist() { return checklist; }

    // ──────────────── Checklist Item ─────────────────────────

    public static class ChecklistItem {

        private String text;
        private boolean checked;

        public ChecklistItem() {}

        public ChecklistItem(String text, boolean checked) {
            this.text = text;
            this.checked = checked;
        }

        public String getText() { return text; }

        public boolean isChecked() { return checked; }
    }
}
