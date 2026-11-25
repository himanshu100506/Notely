package com.notely;

import com.notely.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final double WINDOW_WIDTH = 1000;
    private static final double WINDOW_HEIGHT = 650;

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController();
        controller.initialize();

        Scene scene = new Scene(controller.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("css/style.css").toExternalForm());

        primaryStage.setTitle("Notely — Personal Knowledge Base");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();

        controller.applyStartupTheme();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
