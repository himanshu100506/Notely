package com.notely.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level envelope for a Notely document stored as JSON.
 *
 * <p>Structure:</p>
 * <pre>{@code
 * {
 *   "version": 1,
 *   "blocks": [ ... ]
 * }
 * }</pre>
 *
 * <p>The {@link #CURRENT_VERSION} field enables future format
 * migrations without breaking existing documents.</p>
 */
public class DocumentData {

    public static final int CURRENT_VERSION = 1;

    private int version;
    private List<BlockData> blocks;

    public DocumentData() {
        this.version = CURRENT_VERSION;
        this.blocks = new ArrayList<>();
    }

    public DocumentData(List<BlockData> blocks) {
        this.version = CURRENT_VERSION;
        this.blocks = blocks != null ? new ArrayList<>(blocks) : new ArrayList<>();
    }

    public int getVersion() { return version; }

    public List<BlockData> getBlocks() { return blocks; }

    public void addBlock(BlockData block) {
        blocks.add(block);
    }
}
