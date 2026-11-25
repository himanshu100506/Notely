package com.notely.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsController {

    private static final String PREFS_DIR = System.getProperty("user.home") + "/.notely";
    private static final String PREFS_FILE = PREFS_DIR + "/preferences.properties";
    private static final String KEY_THEME = "theme";

    private final MainController main;
    private final VBox view;
    private String currentTheme;

    public SettingsController(MainController main) {
        this.main = main;
        this.currentTheme = loadThemePreference();

        view = new VBox(24);
        view.setPadding(new Insets(32, 40, 32, 40));
        view.getStyleClass().add("settings-view");

        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");

        VBox themeSection = new VBox(12);
        Label themeHeader = new Label("Appearance");
        themeHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label themeDesc = new Label("Choose your preferred theme");
        themeDesc.setStyle("-fx-text-fill: -notely-text-secondary; -fx-font-size: 13px;");

        ToggleGroup themeGroup = new ToggleGroup();

        RadioButton lightBtn = new RadioButton("Light");
        lightBtn.setToggleGroup(themeGroup);
        lightBtn.getStyleClass().add("settings-radio");

        RadioButton darkBtn = new RadioButton("Dark");
        darkBtn.setToggleGroup(themeGroup);
        darkBtn.getStyleClass().add("settings-radio");

        if ("dark".equals(currentTheme)) {
            darkBtn.setSelected(true);
        } else {
            lightBtn.setSelected(true);
        }

        HBox themeOptions = new HBox(24, lightBtn, darkBtn);
        themeOptions.setAlignment(Pos.CENTER_LEFT);

        themeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == darkBtn) {
                applyTheme("dark");
            } else {
                applyTheme("light");
            }
        });

        themeSection.getChildren().addAll(themeHeader, themeDesc, themeOptions);

        VBox aboutSection = new VBox(8);
        Label aboutHeader = new Label("About");
        aboutHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label version = new Label("Notely v1.0");
        version.setStyle("-fx-text-fill: -notely-text-secondary; -fx-font-size: 13px;");
        Label desc = new Label("A personal knowledge base for your notes, ideas, and thoughts.");
        desc.setStyle("-fx-text-fill: -notely-text-secondary; -fx-font-size: 13px;");
        desc.setWrapText(true);
        aboutSection.getChildren().addAll(aboutHeader, version, desc);

        view.getChildren().addAll(title, themeSection, aboutSection);
    }

    public VBox getView() {
        return view;
    }

    private void applyTheme(String theme) {
        currentTheme = theme;
        saveThemePreference(theme);
        String cssPath = "dark".equals(theme) ? "/com/notely/css/dark-theme.css" : "/com/notely/css/light-theme.css";
        main.applyTheme(cssPath);
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    private String loadThemePreference() {
        Path path = Paths.get(PREFS_FILE);
        if (Files.exists(path)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
                return props.getProperty(KEY_THEME, "light");
            } catch (IOException e) {
                return "light";
            }
        }
        return "light";
    }

    private void saveThemePreference(String theme) {
        try {
            Path dir = Paths.get(PREFS_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Properties props = new Properties();
            props.setProperty(KEY_THEME, theme);
            try (OutputStream out = Files.newOutputStream(Paths.get(PREFS_FILE))) {
                props.store(out, "Notely preferences");
            }
        } catch (IOException e) {
            System.err.println("Could not save preferences: " + e.getMessage());
        }
    }
}
