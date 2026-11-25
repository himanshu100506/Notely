package com.notely.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockSerializerTest {

    // ─────────────── Serialize ───────────────────────────────

    @Test
    void serializeEmptyDocument() {
        DocumentData doc = new DocumentData();
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"blocks\":[]"));
    }

    @Test
    void serializeNullDocumentReturnsEmptyDoc() {
        String json = BlockSerializer.serialize(null);

        assertTrue(json.contains("\"version\":1"));
        assertTrue(json.contains("\"blocks\":[]"));
    }

    @Test
    void serializeParagraph() {
        DocumentData doc = new DocumentData();
        doc.addBlock(BlockData.paragraph("Hello world"));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"paragraph\""));
        assertTrue(json.contains("\"text\":\"Hello world\""));
    }

    @Test
    void serializeH1() {
        DocumentData doc = new DocumentData(List.of(BlockData.h1("Title")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"h1\""));
        assertTrue(json.contains("\"text\":\"Title\""));
    }

    @Test
    void serializeH2() {
        DocumentData doc = new DocumentData(List.of(BlockData.h2("Section")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"h2\""));
    }

    @Test
    void serializeH3() {
        DocumentData doc = new DocumentData(List.of(BlockData.h3("Sub")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"h3\""));
    }

    @Test
    void serializeBulletList() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.bullet(List.of("First", "Second"))));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"bullet\""));
        assertTrue(json.contains("\"items\":[\"First\",\"Second\"]"));
    }

    @Test
    void serializeNumberedList() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.numbered(List.of("Step one", "Step two"))));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"numbered\""));
        assertTrue(json.contains("\"items\":[\"Step one\",\"Step two\"]"));
    }

    @Test
    void serializeChecklist() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.checklist(List.of(
                        new BlockData.ChecklistItem("Done task", true),
                        new BlockData.ChecklistItem("Open task", false)))));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"checklist\""));
        assertTrue(json.contains("\"checked\":true"));
        assertTrue(json.contains("\"checked\":false"));
        assertTrue(json.contains("\"text\":\"Done task\""));
    }

    @Test
    void serializeCode() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.code("int x = 42;")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"code\""));
        assertTrue(json.contains("\"text\":\"int x = 42;\""));
    }

    @Test
    void serializeQuote() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.quote("Stay hungry")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"quote\""));
        assertTrue(json.contains("\"text\":\"Stay hungry\""));
    }

    @Test
    void serializeDivider() {
        DocumentData doc = new DocumentData(List.of(BlockData.divider()));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"divider\""));
        assertFalse(json.contains("\"text\""));
    }

    @Test
    void serializeVersionIncluded() {
        String json = BlockSerializer.serialize(new DocumentData());
        assertTrue(json.contains("\"version\":1"));
    }

    @Test
    void serializeMixedDocument() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.h1("Meeting Notes"),
                BlockData.paragraph("Discussed the roadmap."),
                BlockData.bullet(List.of("Item A", "Item B")),
                BlockData.divider(),
                BlockData.code("System.out.println();")));
        String json = BlockSerializer.serialize(doc);

        assertTrue(json.contains("\"type\":\"h1\""));
        assertTrue(json.contains("\"type\":\"paragraph\""));
        assertTrue(json.contains("\"type\":\"bullet\""));
        assertTrue(json.contains("\"type\":\"divider\""));
        assertTrue(json.contains("\"type\":\"code\""));
    }

    // ─────────────── Deserialize — Valid JSON ────────────────

    @Test
    void deserializeValidDocument() {
        String json = "{\"version\":1,\"blocks\":["
                + "{\"type\":\"paragraph\",\"text\":\"Hello\"}"
                + "]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        assertEquals(1, doc.getVersion());
        assertEquals(1, doc.getBlocks().size());
        assertEquals(BlockType.PARAGRAPH, doc.getBlocks().get(0).getType());
        assertEquals("Hello", doc.getBlocks().get(0).getText());
    }

    @Test
    void deserializeChecklist() {
        String json = "{\"version\":1,\"blocks\":["
                + "{\"type\":\"checklist\",\"checklist\":["
                + "{\"text\":\"Task\",\"checked\":true}"
                + "]}"
                + "]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        BlockData block = doc.getBlocks().get(0);
        assertEquals(BlockType.CHECKLIST, block.getType());
        assertEquals(1, block.getChecklist().size());
        assertEquals("Task", block.getChecklist().get(0).getText());
        assertTrue(block.getChecklist().get(0).isChecked());
    }

    @Test
    void deserializeBulletList() {
        String json = "{\"version\":1,\"blocks\":["
                + "{\"type\":\"bullet\",\"items\":[\"A\",\"B\"]}"
                + "]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        BlockData block = doc.getBlocks().get(0);
        assertEquals(BlockType.BULLET, block.getType());
        assertEquals(List.of("A", "B"), block.getItems());
    }

    // ─────────────── Deserialize — Empty / Null ──────────────

    @Test
    void deserializeNullReturnsEmpty() {
        DocumentData doc = BlockSerializer.deserialize(null);

        assertEquals(DocumentData.CURRENT_VERSION, doc.getVersion());
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void deserializeEmptyStringReturnsEmpty() {
        DocumentData doc = BlockSerializer.deserialize("");
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void deserializeBlankStringReturnsEmpty() {
        DocumentData doc = BlockSerializer.deserialize("   ");
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void deserializeEmptyBlocksListReturnsEmpty() {
        String json = "{\"version\":1,\"blocks\":[]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        assertEquals(1, doc.getVersion());
        assertTrue(doc.getBlocks().isEmpty());
    }

    // ─────────────── Deserialize — Legacy Plain Text ─────────

    @Test
    void deserializeLegacyPlainText() {
        String plainText = "This is my old note content.";
        DocumentData doc = BlockSerializer.deserialize(plainText);

        assertEquals(1, doc.getBlocks().size());
        assertEquals(BlockType.PARAGRAPH, doc.getBlocks().get(0).getType());
        assertEquals(plainText, doc.getBlocks().get(0).getText());
    }

    @Test
    void deserializeLegacyMultilinePlainText() {
        String text = "Line one\nLine two\nLine three";
        DocumentData doc = BlockSerializer.deserialize(text);

        assertEquals(1, doc.getBlocks().size());
        assertEquals(text, doc.getBlocks().get(0).getText());
    }

    // ─────────────── Deserialize — Malformed JSON ────────────

    @Test
    void deserializeMalformedJsonPreservesContent() {
        String bad = "{broken json here";
        DocumentData doc = BlockSerializer.deserialize(bad);

        assertEquals(1, doc.getBlocks().size());
        assertEquals(BlockType.PARAGRAPH, doc.getBlocks().get(0).getType());
        assertEquals(bad, doc.getBlocks().get(0).getText());
    }

    // ─────────────── Deserialize — Unsupported Version ───────

    @Test
    void deserializeUnsupportedVersionThrows() {
        String json = "{\"version\":999,\"blocks\":[]}";

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> BlockSerializer.deserialize(json));
        assertTrue(ex.getMessage().contains("999"));
        assertTrue(ex.getMessage().contains("Unsupported"));
    }

    // ─────────────── Deserialize — Missing Fields ────────────

    @Test
    void deserializeMissingBlocksReturnsEmpty() {
        String json = "{\"version\":1}";
        DocumentData doc = BlockSerializer.deserialize(json);
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void deserializeMissingTextFieldReturnsNull() {
        String json = "{\"version\":1,\"blocks\":["
                + "{\"type\":\"paragraph\"}"
                + "]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        assertNull(doc.getBlocks().get(0).getText());
    }

    @Test
    void deserializeMissingVersionDefaultsToCurrentVersion() {
        String json = "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"hi\"}]}";
        DocumentData doc = BlockSerializer.deserialize(json);

        assertEquals(DocumentData.CURRENT_VERSION, doc.getVersion());
        assertEquals(1, doc.getBlocks().size());
    }

    // ─────────────── Unicode & Special Characters ────────────

    @Test
    void roundTripUnicode() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.paragraph("Héllo wörld 日本語 emoji: 🎉")));
        String json = BlockSerializer.serialize(doc);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals("Héllo wörld 日本語 emoji: 🎉",
                result.getBlocks().get(0).getText());
    }

    @Test
    void roundTripQuotesAndBackslashes() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.paragraph("He said \"hello\" and C:\\path\\file")));
        String json = BlockSerializer.serialize(doc);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals("He said \"hello\" and C:\\path\\file",
                result.getBlocks().get(0).getText());
    }

    @Test
    void roundTripNewlines() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.code("line1\nline2\nline3")));
        String json = BlockSerializer.serialize(doc);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals("line1\nline2\nline3",
                result.getBlocks().get(0).getText());
    }

    @Test
    void roundTripEmptyStrings() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.paragraph("")));
        String json = BlockSerializer.serialize(doc);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals("", result.getBlocks().get(0).getText());
    }

    // ─────────────── Round-Trip ──────────────────────────────

    @Test
    void roundTripAllBlockTypes() {
        DocumentData original = new DocumentData(List.of(
                BlockData.paragraph("Body text"),
                BlockData.h1("Heading 1"),
                BlockData.h2("Heading 2"),
                BlockData.h3("Heading 3"),
                BlockData.bullet(List.of("A", "B")),
                BlockData.numbered(List.of("1st", "2nd")),
                BlockData.checklist(List.of(
                        new BlockData.ChecklistItem("Done", true),
                        new BlockData.ChecklistItem("Todo", false))),
                BlockData.code("x = 1"),
                BlockData.quote("Famous quote"),
                BlockData.divider()));

        String json = BlockSerializer.serialize(original);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals(1, result.getVersion());
        assertEquals(10, result.getBlocks().size());

        assertEquals(BlockType.PARAGRAPH, result.getBlocks().get(0).getType());
        assertEquals("Body text", result.getBlocks().get(0).getText());

        assertEquals(BlockType.H1, result.getBlocks().get(1).getType());
        assertEquals(BlockType.H2, result.getBlocks().get(2).getType());
        assertEquals(BlockType.H3, result.getBlocks().get(3).getType());

        assertEquals(BlockType.BULLET, result.getBlocks().get(4).getType());
        assertEquals(List.of("A", "B"), result.getBlocks().get(4).getItems());

        assertEquals(BlockType.NUMBERED, result.getBlocks().get(5).getType());
        assertEquals(List.of("1st", "2nd"), result.getBlocks().get(5).getItems());

        assertEquals(BlockType.CHECKLIST, result.getBlocks().get(6).getType());
        assertEquals(2, result.getBlocks().get(6).getChecklist().size());
        assertTrue(result.getBlocks().get(6).getChecklist().get(0).isChecked());
        assertFalse(result.getBlocks().get(6).getChecklist().get(1).isChecked());

        assertEquals(BlockType.CODE, result.getBlocks().get(7).getType());
        assertEquals(BlockType.QUOTE, result.getBlocks().get(8).getType());
        assertEquals(BlockType.DIVIDER, result.getBlocks().get(9).getType());
    }

    @Test
    void roundTripPreservesBlockOrder() {
        DocumentData doc = new DocumentData(List.of(
                BlockData.h1("First"),
                BlockData.paragraph("Second"),
                BlockData.code("Third")));
        String json = BlockSerializer.serialize(doc);
        DocumentData result = BlockSerializer.deserialize(json);

        assertEquals("First", result.getBlocks().get(0).getText());
        assertEquals("Second", result.getBlocks().get(1).getText());
        assertEquals("Third", result.getBlocks().get(2).getText());
    }
}
