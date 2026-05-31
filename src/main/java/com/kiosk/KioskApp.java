package com.kiosk;

import com.kiosk.ui.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class KioskApp extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(KioskApp.class.getResource("/fxml/main.fxml"));
        Scene scene =    new Scene(fxmlLoader.load(), 1280, 800);
        scene.getStylesheets().add(
                Objects.requireNonNull(KioskApp.class.getResource("/css/styles.css")).toExternalForm()
        );

        stage.setTitle("Киоск самообслуживания");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.show();

        // Загрузка начального экрана через контроллер
        MainController controller = fxmlLoader.getController();
        controller.initialize();
    }

    public static void main(String[] args) {
        launch();
    }
}
