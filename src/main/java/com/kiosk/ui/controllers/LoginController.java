package com.kiosk.ui.controllers;

import com.kiosk.KioskApp;
import com.kiosk.model.Provider;
import com.kiosk.model.Role;
import com.kiosk.model.Specialization;
import com.kiosk.model.User;
import com.kiosk.service.ApiService;
import com.kiosk.util.AppLogger;
import com.kiosk.util.SessionManager;
import com.kiosk.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private ToggleButton themeToggle;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        // Вход по Enter
        passwordField.setOnAction(e -> onLogin());
    }

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(ThemeManager.isDarkMode());
            themeToggle.setText(ThemeManager.isDarkMode() ? "Светлая тема" : "Темная тема");
        }
    }

    @FXML
    private void onLogin() {
        // Email и пароль берем с формы авторизации, потому что это первый экран приложения.
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
                // Организацию вытягиваем сразу при входе, чтобы дальше фильтровать админку и платежи.
                Provider provider = resolveUserProvider(user);

                SessionManager session = SessionManager.getInstance();
                // Все данные входа кладем в SessionManager, потому что остальные экраны берут их оттуда.
                session.setCurrentUser(user);
                session.setCurrentUserRoles(roles);
                session.setCurrentUserSpecializations(specs);
                if (provider != null) {
                    session.setCurrentProviderId(provider.getId());
                    session.setCurrentProviderName(provider.toString());
                } else {
                    session.setCurrentProviderName(user.getProviderName());
                }
                AppLogger.info("Вход пользователя: " + user.getEmail());

                Platform.runLater(() -> {
                    try {
                        KioskApp.showMain();
                    } catch (Exception e) {
                        showError("Не удалось открыть главный экран: " + e.getMessage());
                        loginBtn.setDisable(false);
                        loginBtn.setText("Войти");
                    }
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
            // Новый ApiAB возвращает роли прямо в UserDto, поэтому сначала читаем пользователя по id.
            User user = ApiService.getUserById(userId);
            if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
                return user.getRoles().stream().map(name -> {
                    Role role = new Role();
                    role.setName(name);
                    return role;
                }).collect(Collectors.toList());
            }
            // Если в UserDto ролей нет, пробуем отдельный endpoint ролей.
            List<Role> roles = ApiService.getRolesByUser(userId);
            if (!roles.isEmpty()) {
                return roles;
            }
        } catch (Exception e) {
            AppLogger.warn("Не удалось загрузить роли пользователя " + userId, e);
        }
        Role defaultRole = new Role();
        // Если роли не пришли, считаем пользователя обычным, чтобы не давать лишние права.
        defaultRole.setName("user");
        return List.of(defaultRole);
    }

    private List<Specialization> loadUserSpecializations(String userId) {
        try {
            // Специализации нужны для ограничения списка услуг обычного пользователя.
            User user = ApiService.getUserById(userId);
            if (user != null && user.getSpecializations() != null) {
                return user.getSpecializations().stream().map(name -> {
                    Specialization specialization = new Specialization();
                    specialization.setName(name);
                    return specialization;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            AppLogger.warn("Не удалось загрузить специализации пользователя " + userId, e);
        }
        return new ArrayList<>();
    }

    private Provider resolveUserProvider(User user) {
        try {
            // Если у пользователя есть provId, точнее всего найти организацию по id.
            if (user.getProvId() != null && !user.getProvId().isBlank()) {
                return ApiService.getProviderById(user.getProvId());
            }
            // В новом DTO может прийти только имя провайдера, тогда сопоставляем его со справочником.
            String providerName = user.getProviderName();
            if (providerName == null || providerName.isBlank()) {
                return null;
            }
            return ApiService.getAllProviders().stream()
                    .filter(provider -> provider.matchesName(providerName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            AppLogger.warn("Не удалось определить организацию пользователя " + user.getEmail(), e);
            return null;
        }
    }

    @FXML
    private void onCancel() {
        Platform.exit();
    }

    @FXML
    private void onThemeToggle() {
        ThemeManager.toggle();
        if (themeToggle != null) {
            themeToggle.setSelected(ThemeManager.isDarkMode());
            themeToggle.setText(ThemeManager.isDarkMode() ? "Светлая тема" : "Темная тема");
        }
        if (stage != null && stage.getScene() != null) {
            ThemeManager.apply(stage.getScene());
        }
    }

    private void showError(String message) {
        errorLabel.setText("! " + message);
        errorLabel.setVisible(true);
    }
}
