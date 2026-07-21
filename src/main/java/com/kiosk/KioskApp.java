package com.kiosk;
import com.kiosk.ui.controllers.LoginController;
import com.kiosk.util.AppLogger;
import com.kiosk.util.DesignManager;
import com.kiosk.util.LocaleManager;
import com.kiosk.util.WindowManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
public class KioskApp extends Application {
    public static Stage primaryStage;
    @Override
    public void start(Stage stage) throws IOException { primaryStage = stage; AppLogger.configure(); showLogin(); }
    public static void showLogin() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(KioskApp.class.getResource("/fxml/login.fxml"));
        fxmlLoader.setResources(LocaleManager.getBundle());
        Scene scene = new Scene(fxmlLoader.load(), 520, 600);
        DesignManager.apply(scene);
        primaryStage.setTitle(LocaleManager.t("app.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(560);
        LoginController controller = fxmlLoader.getController();
        controller.setStage(primaryStage);
        primaryStage.show();
        // Терминал всегда работает на весь экран (в киоск-режиме — без рамки).
        WindowManager.configure(primaryStage);
    }
    public static void showMain() throws IOException {
        // Новый дизайн — макет PAYTERMINAL, старый — классическая раскладка с боковыми панелями.
        String fxml = DesignManager.isNewDesign() ? "/fxml/main.fxml" : "/fxml/main_classic.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader(KioskApp.class.getResource(fxml));
        fxmlLoader.setResources(LocaleManager.getBundle());
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);
        DesignManager.apply(scene);
        primaryStage.setTitle(LocaleManager.t("app.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.show();
        // Сохраняем режим окна (полный экран / киоск) при пересоздании сцены.
        WindowManager.configure(primaryStage);
    }
    public static void main(String[] args) {
        launch();
    }
}
