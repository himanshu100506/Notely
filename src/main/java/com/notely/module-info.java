/**
 * Module descriptor for the Notely application.
 *
 * JavaFX requires explicit module declarations so its runtime
 * can reflectively access controllers and FXML resources.
 */
module com.notely {

    // JavaFX modules this app depends on
    requires javafx.controls;   // UI widgets (Button, ListView, TableView ...)
    requires javafx.fxml;       // FXMLLoader for loading .fxml layouts

    // JDBC — needed to talk to MySQL through java.sql.*
    requires java.sql;

    // Gson — JSON serialization for block-based editor content
    requires com.google.gson;

    // Allow JavaFX to reflectively instantiate controllers declared in FXML
    opens com.notely.controller to javafx.fxml;

    // Allow Gson to reflectively access block data classes for serialization
    opens com.notely.util to com.google.gson;

    // Allow FXML to bind to model properties if needed
    opens com.notely.model to javafx.base;

    // Export packages so they are accessible across the module boundary
    exports com.notely;
    exports com.notely.model;
    exports com.notely.dao;
    exports com.notely.service;
    exports com.notely.controller;
    exports com.notely.util;
}
