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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.Node;
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
import javafx.scene.layout.Priority;
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
                // Справочники нужны для отображения названий и сопоставления провайдера услуги.
                List<Provider> providers = ApiService.getAllProviders();
                List<CategoryService> categories = ApiService.getAllCategories();
                // Обычному пользователю показываем услуги по специализации, администратору — все доступные.
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
        // Фильтр по специализации включаем только для неадминов.
        return session.isLoggedIn() && session.hasSpecializations() && !session.isAdmin();
    }

    private void buildCategoryList() {
        categoryList.getChildren().clear();

        // Кнопка "Все услуги" сбрасывает выбранную категорию.
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
                    // Категории приходят из ApiAB, поэтому строим меню динамически.
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

        // Категорию проверяем по имени и по id, потому что новый DTO часто отдает именно имя.
        if (selectedCategoryName != null && !selectedCategoryName.isBlank()) {
            filtered = filtered.stream()
                    .filter(s -> selectedCategoryName.equalsIgnoreCase(nullToEmpty(s.getCategoryName()))
                            || selectedCategoryId != null && selectedCategoryId.equals(s.getCategoryId()))
                    .collect(Collectors.toList());
        }

        // Поиск идет по услуге, провайдеру и категории, чтобы пользователю было проще найти нужное.
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
            // Окно деталей открываем отдельно, потому что там вводятся сумма и количество.
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

    private void addToCart(ProviderService service, BigDecimal amount, int quantity) {
        // Перед добавлением проверяем провайдера, потому что платеж в ApiAB требует provId.
        Optional<Provider> provider = resolveProvider(service);
        if (provider.isEmpty()) {
            showError("Нужен провайдер",
                    "ApiAB вернул услугу без понятного провайдера. Обновите данные услуги в админке "
                            + "или проверьте справочник провайдеров.");
            return;
        }
        service.setProvId(provider.get().getId());
        cart.add(new CartItem(service, amount, quantity));
        renderCart();
        setStatus("Добавлено к оплате: " + service.getName() + " x" + quantity + " — " + formatMoney(amount.multiply(BigDecimal.valueOf(quantity))));
        AppLogger.info("Услуга добавлена в корзину: " + service.getName() + ", сумма " + amount + ", количество " + quantity);
    }

    private Optional<Provider> resolveProvider(ProviderService service) {
        // Если в услуге уже есть provId, используем его как самый надежный вариант.
        if (service.getProvId() != null && !service.getProvId().isBlank()) {
            return allProviders.stream()
                    .filter(p -> service.getProvId().equals(p.getId()))
                    .findFirst();
        }
        // Если provId нет, сопоставляем провайдера по названию из DTO.
        return allProviders.stream()
                .filter(p -> p.matchesName(service.getProviderName()))
                .findFirst();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart) {
            // Итог корзины складывается из итогов позиций: сумма за единицу * количество.
            total = total.add(item.getTotal());
            VBox row = new VBox(6);
            row.getStyleClass().add("cart-item");

            Label name = new Label(item.getService().getName());
            name.getStyleClass().add("cart-item-title");
            name.setWrapText(true);

            Label meta = new Label(item.getService().getDisplayProvider()
                    + " • " + formatMoney(item.getAmount())
                    + " x " + item.getQuantity()
                    + " = " + formatMoney(item.getTotal()));
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
        // Общий платеж создаем на организацию текущего пользователя.
        String paymentProviderId = paymentProviderId();
        if (groups.isEmpty() || groupedItems != cart.size() || paymentProviderId == null) {
            showError("Нельзя создать платеж",
                    "Не удалось определить организацию для общего платежа. Войдите под пользователем организации "
                            + "или проверьте провайдера у услуг.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Подтверждение платежа");
        styleDialog(dialog, "payment-dialog-pane");
        ButtonType back = new ButtonType("Вернуться к корзине", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Создать платеж", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(back, confirm);

        VBox content = new VBox(16);
        content.getStyleClass().add("payment-shell");
        content.setPadding(new Insets(24));

        Label step = new Label("Шаг 1 из 2");
        step.getStyleClass().add("payment-step");
        Label title = new Label("Подтверждение платежа");
        title.getStyleClass().add("payment-title");
        Label subtitle = new Label("Проверьте услуги, количество и суммы. ApiAB получит один общий платеж на организацию: "
                + providerName(paymentProviderId) + ".");
        subtitle.getStyleClass().add("payment-subtitle");
        subtitle.setWrapText(true);
        content.getChildren().addAll(step, title, subtitle);

        for (Map.Entry<String, List<CartItem>> entry : groups.entrySet()) {
            content.getChildren().add(buildCheckoutProviderBlock(entry.getKey(), entry.getValue()));
        }

        HBox totalRow = new HBox(12);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.getStyleClass().add("payment-total-row");
        Label totalLabel = new Label("Итого к созданию");
        totalLabel.getStyleClass().add("payment-total-label");
        Label totalValue = new Label(formatMoney(cartTotal()));
        totalValue.getStyleClass().add("payment-total-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalLabel, spacer, totalValue);
        content.getChildren().add(totalRow);

        if (groups.size() > 1) {
            Label hint = new Label("В корзине услуги разных провайдеров. ApiAB все равно получит один общий платеж на итоговую сумму.");
            hint.setWrapText(true);
            hint.getStyleClass().add("payment-hint");
            content.getChildren().add(hint);
        }

        content.getChildren().add(new Separator());

        Label payerHeader = new Label("Данные плательщика");
        payerHeader.getStyleClass().add("payment-section-header");

        TextField fioField = new TextField();
        fioField.setPromptText("ФИО плательщика *");

        TextField innField = new TextField();
        innField.setPromptText("ИНН (необязательно)");

        content.getChildren().addAll(payerHeader, fioField, innField);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(680);
        scroll.setPrefHeight(680);
        scroll.getStyleClass().add("payment-scroll");
        dialog.getDialogPane().setContent(scroll);

        Node confirmNode = dialog.getDialogPane().lookupButton(confirm);
        confirmNode.setDisable(true);
        fioField.textProperty().addListener((obs, o, n) -> confirmNode.setDisable(n.isBlank()));

        dialog.setResultConverter(btn -> btn == confirm);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(v ->
                createPayment(paymentProviderId, fioField.getText().trim(),
                        innField.getText().isBlank() ? null : innField.getText().trim()));
    }

    private VBox buildCheckoutProviderBlock(String providerId, List<CartItem> items) {
        VBox block = new VBox(10);
        block.getStyleClass().add("checkout-provider-block");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label provider = new Label(providerName(providerId));
        provider.getStyleClass().add("checkout-provider-name");
        Label count = new Label(items.stream().mapToInt(CartItem::getQuantity).sum() + " шт.");
        count.getStyleClass().add("checkout-count-pill");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label subtotal = new Label(formatMoney(sumItems(items)));
        subtotal.getStyleClass().add("checkout-subtotal");
        header.getChildren().addAll(provider, count, spacer, subtotal);
        block.getChildren().add(header);

        for (CartItem item : items) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label service = new Label(item.getService().getName());
            service.setWrapText(true);
            service.getStyleClass().add("checkout-service-name");
            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            Label amount = new Label(formatMoney(item.getAmount()) + " x " + item.getQuantity()
                    + " = " + formatMoney(item.getTotal()));
            amount.getStyleClass().add("checkout-service-amount");
            row.getChildren().addAll(service, rowSpacer, amount);
            block.getChildren().add(row);
        }
        return block;
    }

    private Map<String, List<CartItem>> groupCartByProvider() {
        Map<String, List<CartItem>> result = new LinkedHashMap<>();
        for (CartItem item : cart) {
            // Группировка нужна только для красивого подтверждения, платеж все равно общий.
            Optional<Provider> provider = resolveProvider(item.getService());
            provider.ifPresent(p -> result.computeIfAbsent(p.getId(), id -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private BigDecimal cartTotal() {
        // Общая сумма платежа — это сумма всех строк корзины.
        return cart.stream()
                .map(CartItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<CartItem> items) {
        return items.stream()
                .map(CartItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createPayment(String providerId, String fio, String inn) {
        payCartBtn.setDisable(true);
        setStatus("Создание платежа в ApiAB...");
        new Thread(() -> {
            try {
                Payment payment = ApiService.createPayment(cartTotal(), providerId, fio, inn);
                List<Payment> created = new ArrayList<>();
                if (payment != null) {
                    created.add(payment);
                    AppLogger.info("Создан общий платеж " + payment.getId() + ", сумма " + payment.getSum());
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
        // После создания платежа показываем QR, который вернул ApiAB.
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("QR-код платежа");
        styleDialog(dialog, "payment-dialog-pane");
        ButtonType close = new ButtonType("Готово", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(close);

        VBox content = new VBox(18);
        content.getStyleClass().add("payment-shell");
        content.setPadding(new Insets(24));

        if (payments.isEmpty()) {
            Label empty = new Label("ApiAB не вернул данные платежа. Проверьте журнал и повторите.");
            empty.getStyleClass().add("payment-error-text");
            empty.setWrapText(true);
            content.getChildren().add(empty);
        }

        Label step = new Label("Шаг 2 из 2");
        step.getStyleClass().add("payment-step");
        Label title = new Label(payments.size() > 1 ? "Платежи созданы" : "Платеж создан");
        title.getStyleClass().add("payment-title");
        Label subtitle = new Label("Покажите QR-код сканеру или используйте данные QR ниже. Не закрывайте окно, пока клиент не завершит оплату.");
        subtitle.getStyleClass().add("payment-subtitle");
        subtitle.setWrapText(true);
        content.getChildren().addAll(step, title, subtitle);

        for (Payment payment : payments) {
            HBox block = new HBox(22);
            block.getStyleClass().add("payment-result");
            block.setAlignment(Pos.CENTER_LEFT);

            VBox qrBox = new VBox(10);
            qrBox.setAlignment(Pos.CENTER);
            qrBox.getStyleClass().add("qr-box");

            if (payment.getQrCode() != null && !payment.getQrCode().isBlank()) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(payment.getQrCode());
                    ImageView qr = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                    qr.setFitWidth(300);
                    qr.setFitHeight(300);
                    qr.setPreserveRatio(true);
                    qr.getStyleClass().add("qr-image");
                    qrBox.getChildren().add(qr);
                } catch (IllegalArgumentException e) {
                    Label noQr = new Label("QR-код поврежден");
                    noQr.getStyleClass().add("payment-error-text");
                    qrBox.getChildren().add(noQr);
                }
            } else {
                Label noQr = new Label("QR-код не получен");
                noQr.getStyleClass().add("payment-error-text");
                qrBox.getChildren().add(noQr);
            }

            Label scanHint = new Label("Сканируйте этот код");
            scanHint.getStyleClass().add("qr-scan-hint");
            qrBox.getChildren().add(scanHint);

            VBox info = new VBox(12);
            info.getStyleClass().add("payment-info");
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label status = new Label(statusLabel(payment.getStatus()));
            status.getStyleClass().addAll("payment-status-pill", statusClass(payment.getStatus()));
            Label provider = new Label(nullToEmpty(payment.getProviderName()).isBlank()
                    ? "Провайдер не указан"
                    : payment.getProviderName());
            provider.getStyleClass().add("payment-provider-name");
            statusRow.getChildren().addAll(status, provider);

            Label id = new Label("ID платежа: " + nullToEmpty(payment.getId()));
            id.getStyleClass().add("payment-muted-line");
            Label fioLabel = new Label("Плательщик: " + nullToEmpty(payment.getFio()));
            fioLabel.getStyleClass().add("payment-muted-line");
            Label amount = new Label("Сумма: " + formatMoney(payment.getSum()));
            amount.getStyleClass().add("payment-amount-line");
            Label fee = new Label("Комиссия: " + formatMoney(payment.getFee()));
            fee.getStyleClass().add("payment-muted-line");
            Label total = new Label("К оплате: " + formatMoney(payment.getTotal()));
            total.getStyleClass().add("payment-grand-total");

            Label qrCaption = new Label("Данные QR");
            qrCaption.getStyleClass().add("payment-qr-caption");
            Label qrData = new Label(nullToEmpty(payment.getQrLink()).isBlank()
                    ? "ApiAB не вернул ссылку QR"
                    : payment.getQrLink());
            qrData.setWrapText(true);
            qrData.getStyleClass().add("payment-qr-data");

            info.getChildren().addAll(statusRow, id, fioLabel);
            if (payment.getInn() != null && !payment.getInn().isBlank()) {
                Label innLabel = new Label("ИНН: " + payment.getInn());
                innLabel.getStyleClass().add("payment-muted-line");
                info.getChildren().add(innLabel);
            }
            info.getChildren().addAll(amount, fee, total, qrCaption, qrData);
            HBox.setHgrow(info, Priority.ALWAYS);

            block.getChildren().addAll(qrBox, info);
            content.getChildren().add(block);
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(820);
        scroll.setPrefHeight(700);
        scroll.getStyleClass().add("payment-scroll");
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog, String styleClass) {
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add(styleClass);
        if (ThemeManager.isDarkMode()) {
            dialog.getDialogPane().getStyleClass().add("dark");
        }
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
        userLabel.setVisible(false);
        userLabel.setManaged(false);
        adminBtn.setVisible(loggedIn && session.isAdmin());

        if (loggedIn) {
            String provider = session.getCurrentProviderName() == null || session.getCurrentProviderName().isBlank()
                    ? ""
                    : " • " + session.getCurrentProviderName();
            setStatus("Вход выполнен: " + session.getCurrentUser().getName() + provider);
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

    private String providerName(String providerId) {
        return allProviders.stream()
                .filter(provider -> Objects.equals(provider.getId(), providerId))
                .findFirst()
                .map(Provider::toString)
                .orElse("Провайдер " + providerId);
    }

    private String paymentProviderId() {
        SessionManager session = SessionManager.getInstance();
        // В первую очередь берем организацию вошедшего пользователя из сессии.
        if (session.hasProvider()) {
            return session.getCurrentProviderId();
        }
        // Запасной вариант: если сессия без провайдера, берем провайдера первой услуги в корзине.
        return cart.stream()
                .map(CartItem::getService)
                .map(this::resolveProvider)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Provider::getId)
                .findFirst()
                .orElse(null);
    }

    private String statusLabel(String status) {
        return switch (nullToEmpty(status)) {
            case "processing" -> "Ожидает оплаты";
            case "success" -> "Оплачен";
            case "cancel" -> "Отменен";
            default -> "Статус неизвестен";
        };
    }

    private String statusClass(String status) {
        return switch (nullToEmpty(status)) {
            case "success" -> "payment-status-success";
            case "cancel" -> "payment-status-cancel";
            default -> "payment-status-processing";
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String readableError(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
