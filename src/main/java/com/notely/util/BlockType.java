package com.notely.util;

import com.google.gson.annotations.SerializedName;

/**
 * Enumerates the supported block types in the Notely editor.
 *
 * <p>Gson serializes each value using its {@link SerializedName}
 * (lowercase), so JSON always contains {@code "paragraph"},
 * {@code "h1"}, etc.</p>
 */
public enum BlockType {

    @SerializedName("paragraph") PARAGRAPH,
    @SerializedName("h1")        H1,
    @SerializedName("h2")        H2,
    @SerializedName("h3")        H3,
    @SerializedName("bullet")    BULLET,
    @SerializedName("numbered")  NUMBERED,
    @SerializedName("checklist") CHECKLIST,
    @SerializedName("code")      CODE,
    @SerializedName("quote")     QUOTE,
    @SerializedName("divider")   DIVIDER
}
