package com.kiosk.ui.controllers;

import com.kiosk.KioskApp;
import com.kiosk.model.CartItem;
import com.kiosk.model.CategoryService;
import com.kiosk.model.Payment;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.service.ApiService;
import com.kiosk.ui.components.CategoryButton;
import com.kiosk.ui.components.ServiceCard;
import com.kiosk.util.AppLogger;
import com.kiosk.util.SessionManager;
import com.kiosk.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TextField searchField;
    @FXML private Button loginBtn, logoutBtn, adminBtn, payCartBtn;
    @FXML private ToggleButton themeToggle;
    @FXML private Label userLabel, breadcrumb, statusLabel, connectionLabel, emptyLabel;
    @FXML private Label featuredLabel, servicesLabel, cartCountLabel, cartTotalLabel, cartHelpLabel;
    @FXML private VBox categoryList, featuredSection, cartItemsBox;
    @FXML private FlowPane featuredPane, servicesPane;

    private List<ProviderService> allServices = new ArrayList<>();
    private List<CategoryService> allCategories = new ArrayList<>();
    private List<Provider> allProviders = new ArrayList<>();
    private final List<CartItem> cart = new ArrayList<>();
    private String selectedCategoryId = null;
    private String selectedCategoryName = null;
    private String searchQuery = "";

    @FXML
    public void initialize() {
        setupSearch();
        setupThemeToggle();
        updateSessionControls();
        renderCart();
        loadData();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            searchQuery = newV.trim().toLowerCase();
            applyFilters();
        });
    }

    private void setupThemeToggle() {
        if (themeToggle != null) {
            themeToggle.setSelected(ThemeManager.isDarkMode());
            themeToggle.setText(ThemeManager.isDarkMode() ? "Светлая тема" : "Темная тема");
        }
    }

    private void loadData() {
        setStatus("Загрузка данных...");
        new Thread(() -> {
            try {
                List<Provider> providers = ApiService.getAllProviders();
                List<CategoryService> categories = ApiService.getAllCategories();
                List<ProviderService> services = shouldLoadUserServices()
                        ? ApiService.getServicesByUser(SessionManager.getInstance().getCurrentUser().getId())
                        : ApiService.getAllServices();

                Platform.runLater(() -> {
                    allProviders = providers;
                    allCategories = categories;
                    allServices = services;
                    buildCategoryList();
                    renderServices(services);
                    renderFeatured(services);
                    setStatus("Загружено: " + services.size() + " услуг, " + categories.size() + " категорий");
                    connectionLabel.setText("● Подключено");
                    connectionLabel.getStyleClass().setAll("status-connected");
                });
            } catch (Exception e) {
                AppLogger.error("Ошибка загрузки данных с ApiAB", e);
                Platform.runLater(() -> {
                    setStatus("Ошибка подключения к API");
                    connectionLabel.setText("● Не подключено");
                    connectionLabel.getStyleClass().setAll("status-disconnected");
                    showError("Ошибка подключения",
                            "Не удалось подключиться к ApiAB на http://localhost:7924.\n"
                                    + "Запустите provider-api-emulator и повторите попытку.\n\n"
                                    + readableError(e));
                });
            }
        }).start();
    }

    private boolean shouldLoadUserServices() {
        SessionManager session = SessionManager.getInstance();
        return session.isLoggedIn() && session.hasSpecializations() && !session.isAdmin();
    }

    private void buildCategoryList() {
        categoryList.getChildren().clear();

        Button allBtn = new Button("Все услуги");
        allBtn.getStyleClass().add("category-btn-active");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setOnAction(e -> {
            selectedCategoryId = null;
            selectedCategoryName = null;
            breadcrumb.setText("Все услуги");
            resetCategoryButtonStyles(allBtn);
            applyFilters();
        });
        categoryList.getChildren().add(allBtn);

        allCategories.stream()
                .sorted((a, b) -> nullToEmpty(a.getName()).compareToIgnoreCase(nullToEmpty(b.getName())))
                .forEach(cat -> {
                    CategoryButton btn = new CategoryButton(cat);
                    btn.setMaxWidth(Double.MAX_VALUE);
                    btn.setOnAction(e -> {
                        selectedCategoryId = cat.getId();
                        selectedCategoryName = cat.getName();
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

    private void applyFilters() {
        List<ProviderService> filtered = allServices;

        if (selectedCategoryName != null && !selectedCategoryName.isBlank()) {
            filtered = filtered.stream()
                    .filter(s -> selectedCategoryName.equalsIgnoreCase(nullToEmpty(s.getCategoryName()))
                            || selectedCategoryId != null && selectedCategoryId.equals(s.getCategoryId()))
                    .collect(Collectors.toList());
        }

        if (!searchQuery.isEmpty()) {
            String q = searchQuery;
            filtered = filtered.stream()
                    .filter(s -> nullToEmpty(s.getName()).toLowerCase().contains(q)
                            || nullToEmpty(s.getProviderName()).toLowerCase().contains(q)
                            || nullToEmpty(s.getCategoryName()).toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        renderServices(filtered);
        servicesLabel.setText(selectedCategoryName == null ? "Все услуги" : selectedCategoryName);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/service_detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Детали услуги");
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
            );
            ThemeManager.apply(scene);

            ServiceDetailController ctrl = loader.getController();
            ctrl.setService(service, allProviders, allCategories);
            ctrl.setOnAddToCart(this::addToCart);
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            AppLogger.warn("Не удалось открыть детали услуги", e);
            showError("Ошибка", "Не удалось открыть детали услуги: " + readableError(e));
        }
    }

    private void addToCart(ProviderService service, BigDecimal amount) {
        Optional<Provider> provider = resolveProvider(service);
        if (provider.isEmpty()) {
            showError("Нужен провайдер",
                    "ApiAB вернул услугу без понятного провайдера. Обновите данные услуги в админке "
                            + "или проверьте справочник провайдеров.");
            return;
        }
        service.setProvId(provider.get().getId());
        cart.add(new CartItem(service, amount));
        renderCart();
        setStatus("Добавлено к оплате: " + service.getName() + " — " + formatMoney(amount));
        AppLogger.info("Услуга добавлена в корзину: " + service.getName() + ", сумма " + amount);
    }

    private Optional<Provider> resolveProvider(ProviderService service) {
        if (service.getProvId() != null && !service.getProvId().isBlank()) {
            return allProviders.stream()
                    .filter(p -> service.getProvId().equals(p.getId()))
                    .findFirst();
        }
        return allProviders.stream()
                .filter(p -> p.matchesName(service.getProviderName()))
                .findFirst();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart) {
            total = total.add(item.getAmount());
            VBox row = new VBox(6);
            row.getStyleClass().add("cart-item");

            Label name = new Label(item.getService().getName());
            name.getStyleClass().add("cart-item-title");
            name.setWrapText(true);

            Label meta = new Label(item.getService().getDisplayProvider() + " • " + formatMoney(item.getAmount()));
            meta.getStyleClass().add("cart-item-meta");

            Button remove = new Button("Убрать");
            remove.getStyleClass().add("btn-small");
            remove.setOnAction(e -> {
                cart.remove(item);
                renderCart();
            });

            row.getChildren().addAll(name, meta, remove);
            cartItemsBox.getChildren().add(row);
        }

        cartCountLabel.setText(String.valueOf(cart.size()));
        cartTotalLabel.setText(formatMoney(total));
        cartHelpLabel.setVisible(cart.isEmpty());
        payCartBtn.setDisable(cart.isEmpty());
    }

    @FXML
    private void onCheckout() {
        if (cart.isEmpty()) {
            showInfo("Корзина пустая", "Сначала выберите услугу и укажите сумму платежа.");
            return;
        }

        Map<String, List<CartItem>> groups = groupCartByProvider();
        int groupedItems = groups.values().stream().mapToInt(List::size).sum();
        if (groups.isEmpty() || groupedItems != cart.size()) {
            showError("Нельзя создать платеж",
                    "Не удалось определить провайдера для услуг в корзине. Проверьте данные услуг.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Подтверждение платежа");
        ButtonType back = new ButtonType("Назад к корзине", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Создать платеж", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(back, confirm);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.getChildren().add(new Label("Проверьте услуги и суммы перед созданием платежа."));

        cart.forEach(item -> content.getChildren().add(new Label(
                item.getService().getName() + " — " + item.getService().getDisplayProvider()
                        + " — " + formatMoney(item.getAmount())
        )));

        content.getChildren().add(new Separator());
        content.getChildren().add(new Label("Итого: " + formatMoney(cartTotal())));
        if (groups.size() > 1) {
            Label hint = new Label("В корзине услуги разных провайдеров, поэтому ApiAB создаст несколько платежей.");
            hint.setWrapText(true);
            hint.getStyleClass().add("detail-hint");
            content.getChildren().add(hint);
        }
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> btn == confirm);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(v -> createPayments(groups));
    }

    private Map<String, List<CartItem>> groupCartByProvider() {
        Map<String, List<CartItem>> result = new LinkedHashMap<>();
        for (CartItem item : cart) {
            Optional<Provider> provider = resolveProvider(item.getService());
            provider.ifPresent(p -> result.computeIfAbsent(p.getId(), id -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private BigDecimal cartTotal() {
        return cart.stream()
                .map(CartItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createPayments(Map<String, List<CartItem>> groups) {
        payCartBtn.setDisable(true);
        setStatus("Создание платежа в ApiAB...");
        new Thread(() -> {
            try {
                List<Payment> created = new ArrayList<>();
                for (Map.Entry<String, List<CartItem>> entry : groups.entrySet()) {
                    BigDecimal sum = entry.getValue().stream()
                            .map(CartItem::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Payment payment = ApiService.createPayment(sum, entry.getKey());
                    if (payment != null) {
                        created.add(payment);
                        AppLogger.info("Создан платеж " + payment.getId() + ", сумма " + payment.getSum());
                    }
                }
                Platform.runLater(() -> {
                    cart.clear();
                    renderCart();
                    setStatus("Платеж создан. Покажите QR-код для оплаты.");
                    showPaymentResult(created);
                });
            } catch (Exception e) {
                AppLogger.error("Ошибка создания платежа", e);
                Platform.runLater(() -> {
                    renderCart();
                    setStatus("Ошибка создания платежа");
                    showError("Платеж не создан",
                            "ApiAB не принял платеж. Проверьте подключение и данные провайдера.\n\n" + readableError(e));
                });
            }
        }).start();
    }

    private void showPaymentResult(List<Payment> payments) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("QR-код платежа");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        if (payments.isEmpty()) {
            content.getChildren().add(new Label("ApiAB не вернул данные платежа. Проверьте журнал и повторите."));
        }

        for (Payment payment : payments) {
            VBox block = new VBox(8);
            block.getStyleClass().add("payment-result");
            block.getChildren().add(new Label("Платеж " + payment.getId()));
            block.getChildren().add(new Label("Провайдер: " + nullToEmpty(payment.getProviderName())));
            block.getChildren().add(new Label("Сумма: " + formatMoney(payment.getSum())
                    + ", комиссия: " + formatMoney(payment.getFee())
                    + ", всего: " + formatMoney(payment.getTotal())));
            block.getChildren().add(new Label("Статус: " + payment.getStatus()));

            if (payment.getQrCode() != null && !payment.getQrCode().isBlank()) {
                byte[] bytes = Base64.getDecoder().decode(payment.getQrCode());
                ImageView qr = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                qr.setFitWidth(220);
                qr.setFitHeight(220);
                qr.setPreserveRatio(true);
                block.getChildren().add(qr);
            }

            Label qrData = new Label("Данные QR: " + nullToEmpty(payment.getQrLink()));
            qrData.setWrapText(true);
            qrData.getStyleClass().add("detail-hint");
            block.getChildren().add(qrData);
            content.getChildren().add(block);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(560);
        scroll.setPrefHeight(560);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    @FXML
    private void onClearCart() {
        cart.clear();
        renderCart();
        setStatus("Корзина очищена");
    }

    @FXML
    private void onThemeToggle() {
        ThemeManager.toggle();
        setupThemeToggle();
        if (themeToggle.getScene() != null) {
            ThemeManager.apply(themeToggle.getScene());
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            KioskApp.showLogin();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось открыть окно входа: " + readableError(e));
        }
    }

    private void updateSessionControls() {
        SessionManager session = SessionManager.getInstance();
        boolean loggedIn = session.isLoggedIn();
        loginBtn.setVisible(!loggedIn);
        logoutBtn.setVisible(loggedIn);
        userLabel.setVisible(loggedIn);
        adminBtn.setVisible(loggedIn && session.isAdmin());

        if (loggedIn) {
            userLabel.setText("Пользователь: " + session.getCurrentUser().getName());
            setStatus("Добро пожаловать, " + session.getCurrentUser().getName() + "!");
        }
    }

    @FXML
    private void onLogoutClicked() {
        String email = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getEmail()
                : "";
        SessionManager.getInstance().logout();
        cart.clear();
        AppLogger.info("Выход пользователя: " + email);
        try {
            KioskApp.showLogin();
        } catch (Exception e) {
            showError("Ошибка", "Не удалось вернуться к авторизации: " + readableError(e));
        }
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
            ThemeManager.apply(scene);
            AdminController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            AppLogger.warn("Не удалось открыть панель администратора", e);
            showError("Ошибка", "Не удалось открыть панель администратора: " + readableError(e));
        }
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        selectedCategoryId = null;
        selectedCategoryName = null;
        breadcrumb.setText("Все услуги");
        if (!categoryList.getChildren().isEmpty()) {
            resetCategoryButtonStyles(categoryList.getChildren().get(0));
        }
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP) + " сом";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String readableError(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
