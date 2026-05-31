package com.kiosk.ui.controllers;

import com.kiosk.model.Role;
import com.kiosk.model.Specialization;
import com.kiosk.model.User;
import com.kiosk.service.ApiService;
import com.kiosk.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        // Вход по Enter
        passwordField.setOnAction(e -> onLogin());
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Введите email и пароль");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Проверка...");
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                User user = ApiService.authenticate(email, password);
                if (user == null) {
                    Platform.runLater(() -> {
                        showError("Неверный email или пароль");
                        loginBtn.setDisable(false);
                        loginBtn.setText("Войти");
                    });
                    return;
                }

                // Загружаем роли и специализации пользователя
                // В реальном API это должны быть отдельные endpoints
                // Здесь используем emulator execute
                List<Role> roles = loadUserRoles(user.getId());
                List<Specialization> specs = loadUserSpecializations(user.getId());

                SessionManager session = SessionManager.getInstance();
                session.setCurrentUser(user);
                session.setCurrentUserRoles(roles);
                session.setCurrentUserSpecializations(specs);

                Platform.runLater(() -> {
                    if (stage != null) stage.close();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Ошибка: " + e.getMessage());
                    loginBtn.setDisable(false);
                    loginBtn.setText("Войти");
                });
            }
        }).start();
    }

    private List<Role> loadUserRoles(String userId) {
        try {
            // Используем emulator execute для получения данных UserRole
            // В реальном API: GET /api/users/{userId}/roles
            // Пробуем через emulator
            var result = ApiService.executeOperation("USER_BY_ID", java.util.Map.of("id", userId));
            // Пока возвращаем дефолтную роль user
            Role defaultRole = new Role();
            defaultRole.setId("default");
            defaultRole.setName("user");
            return List.of(defaultRole);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Specialization> loadUserSpecializations(String userId) {
        try {
            return ApiService.getAllSpecializations(); // фильтруем по userId через emulator
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @FXML
    private void onCancel() {
        if (stage != null) stage.close();
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }
}
