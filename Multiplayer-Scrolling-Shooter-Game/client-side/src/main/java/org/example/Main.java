package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.client.GameClientGUI;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        GameClientGUI gameClientGUI = new GameClientGUI();
        gameClientGUI.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}