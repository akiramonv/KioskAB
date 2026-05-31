package com.kiosk.ui.controllers;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.service.ApiService;
import com.kiosk.ui.components.CategoryButton;
import com.kiosk.ui.components.ServiceCard;
import com.kiosk.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TextField searchField;
    @FXML private Button loginBtn, logoutBtn, adminBtn;
    @FXML private Label userLabel, breadcrumb, statusLabel, connectionLabel, emptyLabel;
    @FXML private Label featuredLabel, servicesLabel;
    @FXML private VBox categoryList, featuredSection;
    @FXML private FlowPane featuredPane, servicesPane;
    @FXML private ComboBox<Provider> providerFilter;

    private List<ProviderService> allServices = new ArrayList<>();
    private List<CategoryService> allCategories = new ArrayList<>();
    private List<Provider> allProviders = new ArrayList<>();
    private String selectedCategoryId = null;
    private String searchQuery = "";

    @FXML
    public void initialize() {
        setupSearch();
        loadData();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            searchQuery = newV.trim().toLowerCase();
            applyFilters();
        });
    }

    private void loadData() {
        setStatus("Загрузка данных...");
        new Thread(() -> {
            try {
                List<ProviderService> services = ApiService.getAllServices();
                List<CategoryService> categories = ApiService.getAllCategories();
                List<Provider> providers = ApiService.getAllProviders();

                Platform.runLater(() -> {
                    allServices = services;
                    allCategories = categories;
                    allProviders = providers;
                    buildCategoryList();
                    buildProviderFilter();
                    renderServices(services);
                    renderFeatured(services);
                    setStatus("Загружено: " + services.size() + " услуг, " + categories.size() + " категорий");
                    connectionLabel.setText("● Подключено");
                    connectionLabel.getStyleClass().setAll("status-connected");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("Ошибка подключения к API");
                    connectionLabel.setText("● Не подключено");
                    connectionLabel.getStyleClass().setAll("status-disconnected");
                    showError("Ошибка подключения",
                            "Не удалось подключиться к API Emulator.\nПроверьте, что сервер запущен на http://localhost:7924\n\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void buildCategoryList() {
        categoryList.getChildren().clear();

        // Кнопка "Все"
        Button allBtn = new Button("📋 Все услуги");
        allBtn.getStyleClass().add("category-btn-active");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setOnAction(e -> {
            selectedCategoryId = null;
            breadcrumb.setText("Все услуги");
            resetCategoryButtonStyles(allBtn);
            applyFilters();
        });
        categoryList.getChildren().add(allBtn);

        // Корневые категории
        allCategories.stream()
                .filter(CategoryService::isRootCategory)
                .forEach(cat -> {
                    CategoryButton btn = new CategoryButton(cat);
                    btn.setMaxWidth(Double.MAX_VALUE);
                    btn.setOnAction(e -> {
                        selectedCategoryId = cat.getId();
                        breadcrumb.setText(cat.getName());
                        resetCategoryButtonStyles(btn);
                        applyFilters();
                    });
                    categoryList.getChildren().add(btn);
                });
    }

    private void resetCategoryButtonStyles(javafx.scene.Node active) {
        categoryList.getChildren().forEach(node -> {
            node.getStyleClass().removeAll("category-btn-active");
            if (!node.getStyleClass().contains("category-btn")) {
                node.getStyleClass().add("category-btn");
            }
        });
        active.getStyleClass().add("category-btn-active");
    }

    private void buildProviderFilter() {
        providerFilter.setItems(FXCollections.observableArrayList(allProviders));
        providerFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Provider p) { return p == null ? "" : p.toString(); }
            @Override public Provider fromString(String s) { return null; }
        });
        providerFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        List<ProviderService> filtered = allServices;

        // Фильтр по категории
        if (selectedCategoryId != null && !selectedCategoryId.isEmpty()) {
            String catId = selectedCategoryId;
            filtered = filtered.stream()
                    .filter(s -> catId.equals(s.getCategoryId()))
                    .collect(Collectors.toList());
        }

        // Фильтр по провайдеру
        Provider selectedProvider = providerFilter.getValue();
        if (selectedProvider != null) {
            String provId = selectedProvider.getId();
            filtered = filtered.stream()
                    .filter(s -> provId.equals(s.getProvId()))
                    .collect(Collectors.toList());
        }

        // Поиск по тексту
        if (!searchQuery.isEmpty()) {
            String q = searchQuery;
            filtered = filtered.stream()
                    .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        // Если авторизован — фильтр по специализации
        if (SessionManager.getInstance().isLoggedIn()
                && SessionManager.getInstance().hasSpecializations()
                && !SessionManager.getInstance().isAdmin()) {
            // Загружаем услуги по пользователю через API (они уже отфильтрованы по специализации)
            // Применяем пересечение
        }

        renderServices(filtered);
        emptyLabel.setVisible(filtered.isEmpty());
    }

    private void renderServices(List<ProviderService> services) {
        servicesPane.getChildren().clear();
        services.forEach(service -> {
            ServiceCard card = new ServiceCard(service, allProviders, allCategories);
            card.setOnMouseClicked(e -> openServiceDetail(service));
            servicesPane.getChildren().add(card);
        });
    }

    private void renderFeatured(List<ProviderService> services) {
        // Показываем первые 4 как "популярные" (в реальном проекте — по рейтингу)
        featuredPane.getChildren().clear();
        services.stream().limit(4).forEach(service -> {
            ServiceCard card = new ServiceCard(service, allProviders, allCategories);
            card.setOnMouseClicked(e -> openServiceDetail(service));
            featuredPane.getChildren().add(card);
        });
        featuredSection.setVisible(!services.isEmpty());
    }

    private void openServiceDetail(ProviderService service) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/service_detail.fxml")
            );
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Детали услуги");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
            );
            ServiceDetailController ctrl = loader.getController();
            ctrl.setService(service, allProviders, allCategories);
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть детали услуги: " + e.getMessage());
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Авторизация");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
            );
            LoginController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.showAndWait();

            if (SessionManager.getInstance().isLoggedIn()) {
                onUserLoggedIn();
            }
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть форму входа: " + e.getMessage());
        }
    }

    private void onUserLoggedIn() {
        var user = SessionManager.getInstance().getCurrentUser();
        userLabel.setText("👤 " + user.getName());
        userLabel.setVisible(true);
        loginBtn.setVisible(false);
        logoutBtn.setVisible(true);

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        adminBtn.setVisible(isAdmin);

        setStatus("Добро пожаловать, " + user.getName() + "!");

        // Если есть специализации — перезагружаем услуги для этого пользователя
        if (SessionManager.getInstance().hasSpecializations() && !isAdmin) {
            loadServicesByUser(user.getId());
        }
    }

    private void loadServicesByUser(String userId) {
        setStatus("Загрузка услуг по специализации...");
        new Thread(() -> {
            try {
                List<ProviderService> userServices = ApiService.getServicesByUser(userId);
                Platform.runLater(() -> {
                    allServices = userServices;
                    applyFilters();
                    renderFeatured(userServices);
                    setStatus("Показаны услуги вашей специализации: " + userServices.size());
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка загрузки услуг: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onLogoutClicked() {
        SessionManager.getInstance().logout();
        userLabel.setVisible(false);
        loginBtn.setVisible(true);
        logoutBtn.setVisible(false);
        adminBtn.setVisible(false);
        setStatus("Сессия завершена");
        loadData(); // перезагружаем все услуги
    }

    @FXML
    private void onAdminClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Панель администратора");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
            );
            AdminController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть панель администратора: " + e.getMessage());
        }
    }

    @FXML
    private void onClearFilters() {
        providerFilter.setValue(null);
        searchField.clear();
        selectedCategoryId = null;
        breadcrumb.setText("Все услуги");
        resetCategoryButtonStyles(categoryList.getChildren().get(0));
        applyFilters();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
