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
import com.kiosk.util.DesignManager;
import com.kiosk.util.LocaleManager;
import com.kiosk.util.SessionManager;
import com.kiosk.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
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

/**
 * Контроллер «классического» (старого) макета: боковая панель категорий,
 * сетка услуг и панель корзины справа. Логика та же, что и раньше, но с меню
 * настроек, переключением дизайна и локализацией ru/en/ky.
 */
public class ClassicMainController {

    @FXML private TextField searchField;
    @FXML private Button payCartBtn;
    @FXML private MenuButton settingsMenu;
    @FXML private MenuItem loginItem, logoutItem, adminItem;
    @FXML private CheckMenuItem darkItem, designItem;
    @FXML private RadioMenuItem langRuItem, langEnItem, langKyItem;
    @FXML private Label breadcrumb, statusLabel, connectionLabel, emptyLabel;
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
        setupMenuState();
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

    // ==================== НАСТРОЙКИ / ТЕМА / ЯЗЫК ====================

    private void setupMenuState() {
        if (darkItem != null) darkItem.setSelected(ThemeManager.isDarkMode());
        if (designItem != null) designItem.setSelected(DesignManager.isNewDesign());
        String lang = LocaleManager.getLanguage();
        if (langRuItem != null) langRuItem.setSelected(LocaleManager.RU.equals(lang));
        if (langEnItem != null) langEnItem.setSelected(LocaleManager.EN.equals(lang));
        if (langKyItem != null) langKyItem.setSelected(LocaleManager.KY.equals(lang));
    }

    @FXML
    private void onThemeToggle() {
        ThemeManager.toggle();
        if (settingsMenu != null && settingsMenu.getScene() != null) {
            DesignManager.apply(settingsMenu.getScene());
        }
        setupMenuState();
    }

    @FXML
    private void onToggleDesign() {
        DesignManager.toggle();
        reloadMain();
    }

    @FXML private void onLangRu() { switchLanguage(LocaleManager.RU); }
    @FXML private void onLangEn() { switchLanguage(LocaleManager.EN); }
    @FXML private void onLangKy() { switchLanguage(LocaleManager.KY); }

    private void switchLanguage(String lang) {
        if (lang.equals(LocaleManager.getLanguage())) {
            return;
        }
        LocaleManager.setLanguage(lang);
        reloadMain();
    }

    private void reloadMain() {
        try {
            KioskApp.showMain();
        } catch (Exception e) {
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    // ==================== ДАННЫЕ ====================

    private void loadData() {
        setStatus(LocaleManager.t("status.loading"));
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
                    setStatus(LocaleManager.t("status.loaded", services.size(), categories.size()));
                    connectionLabel.setText(LocaleManager.t("status.connected"));
                    connectionLabel.getStyleClass().setAll("status-connected");
                });
            } catch (Exception e) {
                AppLogger.error("Ошибка загрузки данных с ApiAB", e);
                Platform.runLater(() -> {
                    setStatus(LocaleManager.t("status.connError"));
                    connectionLabel.setText(LocaleManager.t("status.disconnected"));
                    connectionLabel.getStyleClass().setAll("status-disconnected");
                    showError(LocaleManager.t("status.connError.title"),
                            LocaleManager.t("status.connError.body") + "\n\n" + readableError(e));
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

        Button allBtn = new Button(LocaleManager.t("home.all"));
        allBtn.getStyleClass().add("category-btn-active");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setOnAction(e -> {
            selectedCategoryId = null;
            selectedCategoryName = null;
            breadcrumb.setText(LocaleManager.t("home.all"));
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

    private void resetCategoryButtonStyles(Node active) {
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
        servicesLabel.setText(selectedCategoryName == null ? LocaleManager.t("home.all") : selectedCategoryName);
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
            loader.setResources(LocaleManager.getBundle());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(LocaleManager.t("detail.title"));
            Scene scene = new Scene(loader.load());
            DesignManager.apply(scene);

            ServiceDetailController ctrl = loader.getController();
            ctrl.setService(service, allProviders, allCategories);
            ctrl.setOnAddToCart(this::addToCart);
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            AppLogger.warn("Не удалось открыть детали услуги", e);
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    private void addToCart(ProviderService service, BigDecimal amount, int quantity) {
        Optional<Provider> provider = resolveProvider(service);
        if (provider.isEmpty()) {
            showError(LocaleManager.t("pay.addError.title"), LocaleManager.t("pay.addError.body"));
            return;
        }
        service.setProvId(provider.get().getId());
        cart.add(new CartItem(service, amount, quantity));
        renderCart();
        setStatus(LocaleManager.t("cart.added", service.getName() + " ×" + quantity
                + " — " + formatMoney(amount.multiply(BigDecimal.valueOf(quantity)))));
        AppLogger.info("Услуга добавлена в корзину: " + service.getName());
    }

    private Optional<Provider> resolveProvider(ProviderService service) {
        if (service.getProvId() != null && !service.getProvId().isBlank()) {
            return allProviders.stream().filter(p -> service.getProvId().equals(p.getId())).findFirst();
        }
        return allProviders.stream().filter(p -> p.matchesName(service.getProviderName())).findFirst();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart) {
            total = total.add(item.getTotal());
            VBox row = new VBox(6);
            row.getStyleClass().add("cart-item");

            Label name = new Label(item.getService().getName());
            name.getStyleClass().add("cart-item-title");
            name.setWrapText(true);

            Label meta = new Label(item.getService().getDisplayProvider()
                    + " • " + formatMoney(item.getAmount())
                    + " × " + item.getQuantity()
                    + " = " + formatMoney(item.getTotal()));
            meta.getStyleClass().add("cart-item-meta");

            Button remove = new Button(LocaleManager.t("cart.remove"));
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
            showInfo(LocaleManager.t("pay.emptyCart.title"), LocaleManager.t("pay.emptyCart.body"));
            return;
        }

        Map<String, List<CartItem>> groups = groupCartByProvider();
        int groupedItems = groups.values().stream().mapToInt(List::size).sum();
        String paymentProviderId = paymentProviderId();
        if (groups.isEmpty() || groupedItems != cart.size() || paymentProviderId == null) {
            showError(LocaleManager.t("pay.cantCreate.title"), LocaleManager.t("pay.cantCreate.body"));
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(LocaleManager.t("pay.confirmTitle"));
        styleDialog(dialog, "payment-dialog-pane");
        ButtonType back = new ButtonType(LocaleManager.t("pay.back"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType(LocaleManager.t("pay.create"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(back, confirm);

        VBox content = new VBox(16);
        content.getStyleClass().add("payment-shell");
        content.setPadding(new Insets(24));

        Label step = new Label(LocaleManager.t("pay.step1"));
        step.getStyleClass().add("payment-step");
        Label title = new Label(LocaleManager.t("pay.confirmTitle"));
        title.getStyleClass().add("payment-title");
        Label subtitle = new Label(LocaleManager.t("pay.confirmSubtitle", providerName(paymentProviderId)));
        subtitle.getStyleClass().add("payment-subtitle");
        subtitle.setWrapText(true);
        content.getChildren().addAll(step, title, subtitle);

        for (Map.Entry<String, List<CartItem>> entry : groups.entrySet()) {
            content.getChildren().add(buildCheckoutProviderBlock(entry.getKey(), entry.getValue()));
        }

        HBox totalRow = new HBox(12);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.getStyleClass().add("payment-total-row");
        Label totalLabel = new Label(LocaleManager.t("pay.totalCreate"));
        totalLabel.getStyleClass().add("payment-total-label");
        Label totalValue = new Label(formatMoney(cartTotal()));
        totalValue.getStyleClass().add("payment-total-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalLabel, spacer, totalValue);
        content.getChildren().add(totalRow);

        if (groups.size() > 1) {
            Label hint = new Label(LocaleManager.t("pay.multiHint"));
            hint.setWrapText(true);
            hint.getStyleClass().add("payment-hint");
            content.getChildren().add(hint);
        }

        content.getChildren().add(new Separator());

        Label payerHeader = new Label(LocaleManager.t("pay.payerData"));
        payerHeader.getStyleClass().add("payment-section-header");

        TextField fioField = new TextField();
        fioField.setPromptText(LocaleManager.t("pay.fio"));
        TextField innField = new TextField();
        innField.setPromptText(LocaleManager.t("pay.inn"));

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
        Label count = new Label(LocaleManager.t("cart.positions",
                items.stream().mapToInt(CartItem::getQuantity).sum()));
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
            Label amount = new Label(formatMoney(item.getAmount()) + " × " + item.getQuantity()
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
            Optional<Provider> provider = resolveProvider(item.getService());
            provider.ifPresent(p -> result.computeIfAbsent(p.getId(), id -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private BigDecimal cartTotal() {
        return cart.stream().map(CartItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<CartItem> items) {
        return items.stream().map(CartItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void createPayment(String providerId, String fio, String inn) {
        payCartBtn.setDisable(true);
        setStatus(LocaleManager.t("pay.creating"));
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
                    setStatus(LocaleManager.t("pay.created"));
                    showPaymentResult(created);
                });
            } catch (Exception e) {
                AppLogger.error("Ошибка создания платежа", e);
                Platform.runLater(() -> {
                    renderCart();
                    setStatus(LocaleManager.t("pay.notCreated.title"));
                    showError(LocaleManager.t("pay.notCreated.title"),
                            LocaleManager.t("pay.notCreated.body") + "\n\n" + readableError(e));
                });
            }
        }).start();
    }

    private void showPaymentResult(List<Payment> payments) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(LocaleManager.t("pay.createdTitle"));
        styleDialog(dialog, "payment-dialog-pane");
        ButtonType close = new ButtonType(LocaleManager.t("pay.done"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(close);

        VBox content = new VBox(18);
        content.getStyleClass().add("payment-shell");
        content.setPadding(new Insets(24));

        if (payments.isEmpty()) {
            Label empty = new Label(LocaleManager.t("pay.noData"));
            empty.getStyleClass().add("payment-error-text");
            empty.setWrapText(true);
            content.getChildren().add(empty);
        }

        Label step = new Label(LocaleManager.t("pay.step2"));
        step.getStyleClass().add("payment-step");
        Label title = new Label(payments.size() > 1
                ? LocaleManager.t("pay.createdTitleMulti") : LocaleManager.t("pay.createdTitle"));
        title.getStyleClass().add("payment-title");
        Label subtitle = new Label(LocaleManager.t("pay.createdSubtitle"));
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
                    Label noQr = new Label(LocaleManager.t("pay.qrBroken"));
                    noQr.getStyleClass().add("payment-error-text");
                    qrBox.getChildren().add(noQr);
                }
            } else {
                Label noQr = new Label(LocaleManager.t("pay.qrNoCode"));
                noQr.getStyleClass().add("payment-error-text");
                qrBox.getChildren().add(noQr);
            }

            Label scanHint = new Label(LocaleManager.t("pay.scanHint"));
            scanHint.getStyleClass().add("qr-scan-hint");
            qrBox.getChildren().add(scanHint);

            VBox info = new VBox(12);
            info.getStyleClass().add("payment-info");
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label status = new Label(statusLabel(payment.getStatus()));
            status.getStyleClass().addAll("payment-status-pill", statusClass(payment.getStatus()));
            Label provider = new Label(nullToEmpty(payment.getProviderName()).isBlank()
                    ? LocaleManager.t("pay.providerNone") : payment.getProviderName());
            provider.getStyleClass().add("payment-provider-name");
            statusRow.getChildren().addAll(status, provider);

            Label id = new Label(LocaleManager.t("pay.id", nullToEmpty(payment.getId())));
            id.getStyleClass().add("payment-muted-line");
            Label fioLabel = new Label(LocaleManager.t("pay.payer", nullToEmpty(payment.getFio())));
            fioLabel.getStyleClass().add("payment-muted-line");
            Label amount = new Label(LocaleManager.t("pay.amount", formatMoney(payment.getSum())));
            amount.getStyleClass().add("payment-amount-line");
            Label fee = new Label(LocaleManager.t("pay.fee", formatMoney(payment.getFee())));
            fee.getStyleClass().add("payment-muted-line");
            Label total = new Label(LocaleManager.t("pay.toPay", formatMoney(payment.getTotal())));
            total.getStyleClass().add("payment-grand-total");

            Label qrCaption = new Label(LocaleManager.t("pay.qrCaption"));
            qrCaption.getStyleClass().add("payment-qr-caption");
            Label qrData = new Label(nullToEmpty(payment.getQrLink()).isBlank()
                    ? LocaleManager.t("pay.qrNoLink") : payment.getQrLink());
            qrData.setWrapText(true);
            qrData.getStyleClass().add("payment-qr-data");

            info.getChildren().addAll(statusRow, id, fioLabel);
            if (payment.getInn() != null && !payment.getInn().isBlank()) {
                Label innLabel = new Label(LocaleManager.t("pay.innLine", payment.getInn()));
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
        DesignManager.apply(dialog.getDialogPane());
        dialog.getDialogPane().getStyleClass().add(styleClass);
        if (ThemeManager.isDarkMode()) {
            dialog.getDialogPane().getStyleClass().add("dark");
        }
    }

    @FXML
    private void onClearCart() {
        cart.clear();
        renderCart();
        setStatus(LocaleManager.t("cart.cleared"));
    }

    @FXML
    private void onLoginClicked() {
        try {
            KioskApp.showLogin();
        } catch (Exception e) {
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    private void updateSessionControls() {
        SessionManager session = SessionManager.getInstance();
        boolean loggedIn = session.isLoggedIn();
        loginItem.setVisible(!loggedIn);
        logoutItem.setVisible(loggedIn);
        adminItem.setVisible(loggedIn && session.isAdmin());

        if (loggedIn) {
            setStatus(LocaleManager.t("status.loggedIn", session.getCurrentUser().getName()));
        }
    }

    @FXML
    private void onLogoutClicked() {
        String email = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getEmail() : "";
        SessionManager.getInstance().logout();
        cart.clear();
        AppLogger.info("Выход пользователя: " + email);
        try {
            KioskApp.showLogin();
        } catch (Exception e) {
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    @FXML
    private void onAdminClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
            loader.setResources(LocaleManager.getBundle());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(LocaleManager.t("admin.title"));
            Scene scene = new Scene(loader.load());
            DesignManager.apply(scene);
            AdminController ctrl = loader.getController();
            ctrl.setStage(stage);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            AppLogger.warn("Не удалось открыть панель администратора", e);
            showError(LocaleManager.t("common.error"), LocaleManager.t("admin.open.error") + " " + readableError(e));
        }
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        selectedCategoryId = null;
        selectedCategoryName = null;
        breadcrumb.setText(LocaleManager.t("home.all"));
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
        return value.setScale(2, RoundingMode.HALF_UP) + " " + LocaleManager.t("money.suffix");
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
        if (session.hasProvider()) {
            return session.getCurrentProviderId();
        }
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
            case "processing" -> LocaleManager.t("pay.statusProcessing");
            case "success" -> LocaleManager.t("pay.statusSuccess");
            case "cancel" -> LocaleManager.t("pay.statusCancel");
            default -> LocaleManager.t("pay.statusUnknown");
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
