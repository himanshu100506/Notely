package com.notely.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Serializes and deserializes {@link DocumentData} to/from JSON
 * stored in the note's {@code content} column.
 *
 * <h3>Backward compatibility</h3>
 * <ul>
 *   <li>Legacy plain text (not JSON) is wrapped in a single
 *       paragraph block so existing notes remain readable.</li>
 *   <li>Malformed JSON is also preserved as a paragraph block
 *       — content is never silently discarded.</li>
 *   <li>Unsupported future versions throw
 *       {@link IllegalArgumentException}.</li>
 * </ul>
 *
 * <p>This class has no JavaFX dependency and no database access.</p>
 */
public final class BlockSerializer {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private BlockSerializer() {}

    public static String serialize(DocumentData document) {
        if (document == null) {
            return GSON.toJson(new DocumentData());
        }
        return GSON.toJson(document);
    }

    public static DocumentData deserialize(String content) {
        if (content == null || content.isBlank()) {
            return new DocumentData();
        }

        String trimmed = content.trim();

        if (trimmed.startsWith("{")) {
            try {
                DocumentData doc = GSON.fromJson(trimmed, DocumentData.class);
                if (doc == null) {
                    return new DocumentData();
                }
                if (doc.getVersion() > DocumentData.CURRENT_VERSION) {
                    throw new IllegalArgumentException(
                            "Unsupported document version: " + doc.getVersion()
                          + " (max supported: " + DocumentData.CURRENT_VERSION + ")");
                }
                if (doc.getBlocks() == null) {
                    return new DocumentData();
                }
                return doc;
            } catch (JsonSyntaxException e) {
                return wrapAsLegacy(content);
            }
        }

        return wrapAsLegacy(content);
    }

    private static DocumentData wrapAsLegacy(String content) {
        DocumentData doc = new DocumentData();
        doc.addBlock(BlockData.paragraph(content));
        return doc;
    }
}
