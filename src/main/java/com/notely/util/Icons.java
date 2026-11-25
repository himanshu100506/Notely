package com.notely.util;

import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

/**
 * Centralized SVG icon definitions for the Notely UI.
 *
 * <p>Each constant holds SVG path data for a 24×24 viewport.
 * Factory methods return a {@link Region} shaped by the path,
 * styled via CSS class {@code icon}.  The region's background
 * color fills the icon shape, so recoloring is done with
 * {@code -fx-background-color} in CSS.</p>
 */
public final class Icons {

    private Icons() {}

    public static final double DEFAULT_SIZE = 18;

    // ──────────────── Path Data (24×24 viewport) ─────────────

    public static final String SEARCH =
            "M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 "
          + "6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 "
          + "4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 "
          + "9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";

    public static final String HOME =
            "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";

    public static final String DOCUMENT =
            "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 "
          + "2-.9 2-2V8l-6-6zm-1 7V3.5L18.5 9H13z";

    public static final String STAR =
            "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 "
          + "9.19 8.63 2 9.24l5.46 4.73L5.82 21z";

    public static final String STAR_OUTLINE =
            "M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 "
          + "21 12 17.27 18.18 21l-1.64-7.03L22 9.24zM12 15.4l-3.76 "
          + "2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38"
          + "-3.32 2.88 1 4.28L12 15.4z";

    public static final String FOLDER =
            "M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 "
          + "2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z";

    public static final String TAG =
            "M21.41 11.58l-9-9C12.04 2.21 11.53 2 11 2H4c-1.1 0-2 "
          + ".9-2 2v7c0 .53.21 1.04.59 1.41l9 9c.36.36.86.59 1.41.59"
          + ".55 0 1.05-.23 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55"
          + "-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 "
          + "5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";

    public static final String SETTINGS =
            "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94"
          + "l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37"
          + "-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54"
          + "c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 "
          + "2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59"
          + ".22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07"
          + ".62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61"
          + "l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62"
          + ".94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41"
          + "l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 "
          + ".59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 "
          + "15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z";

    public static final String PLUS =
            "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";

    public static final String PIN =
            "M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z";

    public static final String BACK =
            "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";

    public static final String CHEVRON_RIGHT =
            "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z";

    public static final String CHEVRON_DOWN =
            "M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z";

    public static final String DELETE =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5"
          + "l-1-1h-5l-1 1H5v2h14V4z";

    public static final String EDIT =
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 "
          + "7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39"
          + "-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";

    public static final String CHECK =
            "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";

    public static final String CLOSE =
            "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 "
          + "17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";

    public static final String MORE_HORIZONTAL =
            "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 "
          + "0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1"
          + " 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    // ──────────────── Factory Methods ────────────────────────

    public static Region icon(String svgPathData, double size) {
        SVGPath shape = new SVGPath();
        shape.setContent(svgPathData);
        Region region = new Region();
        region.setShape(shape);
        region.setMinSize(size, size);
        region.setMaxSize(size, size);
        region.setPrefSize(size, size);
        region.getStyleClass().add("icon");
        return region;
    }

    public static Region icon(String svgPathData) {
        return icon(svgPathData, DEFAULT_SIZE);
    }

    public static Region search()         { return icon(SEARCH); }
    public static Region home()           { return icon(HOME); }
    public static Region document()       { return icon(DOCUMENT); }
    public static Region star()           { return icon(STAR); }
    public static Region starOutline()    { return icon(STAR_OUTLINE); }
    public static Region folder()         { return icon(FOLDER); }
    public static Region tag()            { return icon(TAG); }
    public static Region settings()       { return icon(SETTINGS); }
    public static Region plus()           { return icon(PLUS); }
    public static Region pin()            { return icon(PIN); }
    public static Region back()           { return icon(BACK); }
    public static Region chevronRight()   { return icon(CHEVRON_RIGHT); }
    public static Region chevronDown()    { return icon(CHEVRON_DOWN); }
    public static Region delete()         { return icon(DELETE); }
    public static Region edit()           { return icon(EDIT); }
    public static Region check()          { return icon(CHECK); }
    public static Region close()          { return icon(CLOSE); }
    public static Region moreHorizontal() { return icon(MORE_HORIZONTAL); }
}
