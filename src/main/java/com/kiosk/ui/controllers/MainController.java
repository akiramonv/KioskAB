package com.kiosk.ui.controllers;

import com.kiosk.KioskApp;
import com.kiosk.api.ApiClient;
import com.kiosk.model.CartItem;
import com.kiosk.model.CategoryService;
import com.kiosk.model.Payment;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.service.ApiService;
import com.kiosk.ui.components.ServiceCard;
import com.kiosk.util.AppLogger;
import com.kiosk.util.DesignManager;
import com.kiosk.util.Fx;
import com.kiosk.util.LocaleManager;
import com.kiosk.util.SessionManager;
import com.kiosk.util.ThemeManager;
import com.kiosk.util.WindowManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер нового дизайна (макет «Киоск — новый дизайн»).
 * Все шаги оплаты — полноэкранные разделы внутри одного окна:
 * главный → услуги → корзина → подтверждение (шаг 1) → QR (шаг 2).
 * Детали услуги открываются нижней шторкой с затемнением, события — тостами.
 */
public class MainController {

    @FXML private StackPane rootStack;
    @FXML private TextField homeSearchField, servicesSearchField;
    @FXML private Button payCartBtn, cartSummaryBtn, checkoutBtn, createPaymentBtn;
    @FXML private MenuButton settingsMenu;
    @FXML private MenuItem loginItem, logoutItem, adminItem;
    @FXML private CheckMenuItem darkItem, designItem, kioskItem;
    @FXML private RadioMenuItem langRuItem, langEnItem, langKyItem;
    @FXML private Label clockLabel, dateLabel;
    @FXML private Label servicesTitle, statusLabel, connectionLabel, emptyLabel;
    @FXML private FlowPane servicesPane;
    @FXML private VBox categoryList;
    @FXML private ScrollPane homeView, qrView;
    @FXML private VBox servicesView, cartView, checkoutView;
    @FXML private HBox cartBar;

    // Корзина
    @FXML private VBox cartItemsBox;
    @FXML private Label cartEmptyLabel, cartTotalLabel;

    // Подтверждение
    @FXML private TextField payerFioField, payerInnField;
    @FXML private Label payerErrorLabel;
    @FXML private VBox checkoutItemsBox;

    // QR
    @FXML private ImageView qrImageView;
    @FXML private Label qrErrorLabel;
    @FXML private VBox qrInfoBox;
    @FXML private HBox payStatusPill;
    @FXML private Label payStatusDot, payStatusText;

    // Шторка и тост
    @FXML private StackPane overlayPane;
    @FXML private Region dimRegion;
    @FXML private VBox sheetBox;
    @FXML private Label toastLabel;

    private List<ProviderService> allServices = new ArrayList<>();
    private List<CategoryService> allCategories = new ArrayList<>();
    private List<Provider> allProviders = new ArrayList<>();
    private final List<CartItem> cart = new ArrayList<>();
    private String selectedCategoryId = null;
    private String selectedCategoryName = null;
    private String searchQuery = "";

    private Node cartReturnTarget;
    private Animation toastAnim;
    private Timeline payPoll;
    private Timeline pulseAnim;
    private Payment currentPayment;

    // Один общий таймер часов на приложение, чтобы при пересоздании экрана не плодить копии.
    private static Timeline clock;

    @FXML
    public void initialize() {
        setupSearch();
        setupMenuState();
        startClock();
        updateSessionControls();
        renderCartBar(false);
        showHome();
        loadData();
    }

    // ==================== ЧАСЫ ====================

    private void startClock() {
        if (clock != null) {
            clock.stop();
        }
        updateClock();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updateClock() {
        if (clockLabel == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        clockLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", LocaleManager.getLocale())));
    }

    // ==================== ЭКРАНЫ / НАВИГАЦИЯ ====================

    private List<Node> screens() {
        return List.of(homeView, servicesView, cartView, checkoutView, qrView);
    }

    private Node currentScreen() {
        return screens().stream().filter(Node::isVisible).findFirst().orElse(homeView);
    }

    /** Переключает экран с анимацией screenIn из макета. */
    private void showScreen(Node target) {
        if (target != qrView) {
            stopPaymentTracking();
        }
        for (Node screen : screens()) {
            boolean active = screen == target;
            screen.setVisible(active);
            screen.setManaged(active);
        }
        // Как в макете: панель корзины видна только на главном и в списке услуг.
        renderCartBar(false);
        Fx.screenIn(target);
    }

    private void setupSearch() {
        homeSearchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.trim();
            if (!q.isEmpty()) {
                // Поиск с домашнего экрана уводит в список услуг и продолжается там же.
                selectedCategoryId = null;
                selectedCategoryName = null;
                searchQuery = q.toLowerCase();
                servicesSearchField.setText(q);
                showServices(LocaleManager.t("home.all"));
                servicesSearchField.requestFocus();
                servicesSearchField.positionCaret(q.length());
                homeSearchField.clear();
            }
        });
        servicesSearchField.textProperty().addListener((obs, oldV, newV) -> {
            searchQuery = newV.trim().toLowerCase();
            applyFilters();
        });
    }

    private void showHome() {
        showScreen(homeView);
        selectedCategoryId = null;
        selectedCategoryName = null;
        searchQuery = "";
        servicesSearchField.clear();
    }

    private void showServices(String title) {
        servicesTitle.setText(title);
        showScreen(servicesView);
        applyFilters();
    }

    @FXML
    private void onBackHome() {
        showHome();
    }

    // ==================== НАСТРОЙКИ / ТЕМА / ЯЗЫК ====================

    private void setupMenuState() {
        if (darkItem != null) {
            darkItem.setSelected(ThemeManager.isDarkMode());
        }
        if (designItem != null) {
            designItem.setSelected(DesignManager.isNewDesign());
        }
        if (kioskItem != null) {
            kioskItem.setSelected(WindowManager.isKioskMode());
        }
        String lang = LocaleManager.getLanguage();
        if (langRuItem != null) langRuItem.setSelected(LocaleManager.RU.equals(lang));
        if (langEnItem != null) langEnItem.setSelected(LocaleManager.EN.equals(lang));
        if (langKyItem != null) langKyItem.setSelected(LocaleManager.KY.equals(lang));
    }

    @FXML
    private void onThemeToggle() {
        ThemeManager.toggle();
        reapplyStyles();
        setupMenuState();
    }

    @FXML
    private void onToggleDesign() {
        DesignManager.toggle();
        // Старый и новый дизайн используют разные макеты, поэтому перезагружаем экран целиком.
        try {
            KioskApp.showMain();
        } catch (Exception e) {
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    @FXML
    private void onToggleKiosk() {
        // Безграничный режим прячет рамку с кнопкой закрытия у текущего окна.
        WindowManager.toggle();
        setupMenuState();
    }

    @FXML private void onLangRu() { switchLanguage(LocaleManager.RU); }
    @FXML private void onLangEn() { switchLanguage(LocaleManager.EN); }
    @FXML private void onLangKy() { switchLanguage(LocaleManager.KY); }

    private void switchLanguage(String lang) {
        if (lang.equals(LocaleManager.getLanguage())) {
            return;
        }
        LocaleManager.setLanguage(lang);
        try {
            // Перезагружаем экран целиком, чтобы перечитать все %ключи из FXML.
            KioskApp.showMain();
        } catch (Exception e) {
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    private void reapplyStyles() {
        if (settingsMenu != null && settingsMenu.getScene() != null) {
            DesignManager.apply(settingsMenu.getScene());
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
                    buildCategoryRows();
                    setStatus(LocaleManager.t("status.loaded", services.size(), categories.size()));
                    if (ApiClient.isDemoMode()) {
                        // Эмулятор недоступен — работаем на локальных демо-данных.
                        connectionLabel.setText(LocaleManager.t("status.demo"));
                        connectionLabel.getStyleClass().setAll("status-demo");
                    } else {
                        connectionLabel.setText(LocaleManager.t("status.connected"));
                        connectionLabel.getStyleClass().setAll("status-connected");
                    }
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

    // ==================== СПИСОК КАТЕГОРИЙ ====================

    private void buildCategoryRows() {
        categoryList.getChildren().clear();

        // Строка "Все услуги" открывает полный список.
        categoryList.getChildren().add(buildCategoryRow(1, "◎", LocaleManager.t("home.all"),
                allServices.size(), () -> {
                    selectedCategoryId = null;
                    selectedCategoryName = null;
                    showServices(LocaleManager.t("home.all"));
                }));

        List<CategoryService> sorted = allCategories.stream()
                .sorted((a, b) -> nullToEmpty(a.getName()).compareToIgnoreCase(nullToEmpty(b.getName())))
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            CategoryService cat = sorted.get(i);
            categoryList.getChildren().add(buildCategoryRow(i + 2, iconFor(cat.getName()), cat.getName(),
                    countServicesIn(cat), () -> {
                        selectedCategoryId = cat.getId();
                        selectedCategoryName = cat.getName();
                        showServices(cat.getName());
                    }));
        }

        // Каскадное появление строк, как rowIn в макете.
        List<Node> rows = new ArrayList<>(categoryList.getChildren());
        for (int i = 0; i < rows.size(); i++) {
            Fx.rowIn(rows.get(i), i);
        }
    }

    private HBox buildCategoryRow(int index, String icon, String name, long count, Runnable onClick) {
        HBox row = new HBox(16);
        row.getStyleClass().add("cat-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label idxLabel = new Label(String.format("%02d", index));
        idxLabel.getStyleClass().add("cat-row-idx");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("cat-row-icon");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("cat-row-name");
        Label countLabel = new Label(LocaleManager.t("home.count", count));
        countLabel.getStyleClass().add("cat-row-count");
        VBox text = new VBox(2, nameLabel, countLabel);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("cat-row-arrow");

        row.getChildren().addAll(idxLabel, iconLabel, text, arrow);
        row.setOnMouseClicked(e -> onClick.run());
        return row;
    }

    private long countServicesIn(CategoryService cat) {
        return allServices.stream()
                .filter(s -> nullToEmpty(cat.getName()).equalsIgnoreCase(nullToEmpty(s.getCategoryName()))
                        || cat.getId() != null && cat.getId().equals(s.getCategoryId()))
                .count();
    }

    /** Эмодзи-иконка для категории по ключевым словам в названии. */
    private String iconFor(String name) {
        String n = nullToEmpty(name).toLowerCase();
        if (n.contains("интернет") || n.contains("internet")) return "🌐";
        if (n.contains("моб") || n.contains("связ") || n.contains("mobile") || n.contains("phone")) return "📱";
        if (n.contains("комм") || n.contains("вод") || n.contains("газ") || n.contains("свет")
                || n.contains("utilit")) return "💧";
        if (n.contains("тв") || n.contains("телев") || n.contains("tv")) return "📺";
        if (n.contains("образ") || n.contains("educat") || n.contains("школ") || n.contains("универ")) return "🎓";
        if (n.contains("налог") || n.contains("tax")) return "🏛";
        if (n.contains("штраф") || n.contains("fine") || n.contains("гаи") || n.contains("полиц")) return "🚓";
        if (n.contains("банк") || n.contains("кредит") || n.contains("bank") || n.contains("loan")) return "🏦";
        if (n.contains("игр") || n.contains("game")) return "🎮";
        if (n.contains("транспорт") || n.contains("такси") || n.contains("transport")) return "🚌";
        return "💳";
    }

    // ==================== ФИЛЬТР / СПИСОК УСЛУГ ====================

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
        emptyLabel.setVisible(filtered.isEmpty());
    }

    private void renderServices(List<ProviderService> services) {
        servicesPane.getChildren().clear();
        int index = 0;
        for (ProviderService service : services) {
            ServiceCard card = new ServiceCard(service, allProviders, allCategories);
            card.setOnMouseClicked(e -> openServiceDetail(service));
            servicesPane.getChildren().add(card);
            Fx.rowIn(card, index++);
        }
    }

    // ==================== ШТОРКА ДЕТАЛЕЙ ====================

    private void openServiceDetail(ProviderService service) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/service_detail.fxml"));
            loader.setResources(LocaleManager.getBundle());
            Parent sheetRoot = loader.load();

            ServiceDetailController ctrl = loader.getController();
            ctrl.setService(service, allProviders, allCategories);
            ctrl.setOnAddToCart(this::addToCart);
            ctrl.setOnClose(this::closeSheet);

            sheetBox.getChildren().setAll(sheetRoot);
            overlayPane.setVisible(true);
            Fx.dimIn(dimRegion);
            Fx.sheetUp(sheetBox);
        } catch (Exception e) {
            AppLogger.warn("Не удалось открыть детали услуги", e);
            showError(LocaleManager.t("common.error"), readableError(e));
        }
    }

    private void closeSheet() {
        if (!overlayPane.isVisible()) {
            return;
        }
        Fx.sheetDown(sheetBox, () -> {
            overlayPane.setVisible(false);
            sheetBox.getChildren().clear();
        });
    }

    @FXML
    private void onDimClicked() {
        closeSheet();
    }

    // ==================== КОРЗИНА ====================

    private void addToCart(ProviderService service, BigDecimal amount, int quantity) {
        Optional<Provider> provider = resolveProvider(service);
        if (provider.isEmpty()) {
            showError(LocaleManager.t("pay.addError.title"), LocaleManager.t("pay.addError.body"));
            return;
        }
        service.setProvId(provider.get().getId());
        cart.add(new CartItem(service, amount, quantity));
        renderCartBar(true);
        showToast(LocaleManager.t("cart.added", service.getName()));
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

    /** Обновляет нижнюю панель корзины; при первом появлении панель выезжает снизу. */
    private void renderCartBar(boolean animateIn) {
        BigDecimal total = cartTotal();
        Node current = currentScreen();
        // Как в макете: на шагах корзины/оплаты панель не дублирует экран.
        boolean show = !cart.isEmpty() && (current == homeView || current == servicesView);
        boolean wasHidden = !cartBar.isVisible();
        cartBar.setVisible(show);
        cartBar.setManaged(show);
        payCartBtn.setDisable(cart.isEmpty());
        cartSummaryBtn.setText(LocaleManager.t("cart.positions", cart.size()) + "  •  " + formatMoney(total));
        if (show && wasHidden && animateIn) {
            Fx.barIn(cartBar);
        }
        // Если экран корзины открыт — обновляем и его.
        if (cartView.isVisible()) {
            renderCartScreen();
        }
    }

    @FXML
    private void onShowCart() {
        Node current = currentScreen();
        if (current == homeView || current == servicesView) {
            cartReturnTarget = current;
        } else if (cartReturnTarget == null) {
            cartReturnTarget = homeView;
        }
        renderCartScreen();
        showScreen(cartView);
    }

    @FXML
    private void onBackFromCart() {
        showScreen(cartReturnTarget != null ? cartReturnTarget : homeView);
    }

    private void renderCartScreen() {
        cartItemsBox.getChildren().clear();
        boolean empty = cart.isEmpty();
        cartEmptyLabel.setVisible(empty);
        cartEmptyLabel.setManaged(empty);
        checkoutBtn.setDisable(empty);

        int index = 0;
        for (CartItem item : new ArrayList<>(cart)) {
            Node row = buildCartRow(item);
            cartItemsBox.getChildren().add(row);
            Fx.rowIn(row, index++);
        }
        cartTotalLabel.setText(formatMoney(cartTotal()));
    }

    private Node buildCartRow(CartItem item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("cart-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(item.getService().getName());
        name.getStyleClass().add("cart-row-name");
        name.setWrapText(true);
        Label meta = new Label(item.getService().getDisplayProvider()
                + " · " + formatMoney(item.getAmount()));
        meta.getStyleClass().add("cart-row-meta");
        VBox text = new VBox(3, name, meta);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button minus = new Button("−");
        minus.getStyleClass().add("qty-btn");
        minus.setOnAction(e -> changeQuantity(item, -1));
        Label qty = new Label(String.valueOf(item.getQuantity()));
        qty.getStyleClass().add("qty-value");
        Button plus = new Button("+");
        plus.getStyleClass().add("qty-btn");
        plus.setOnAction(e -> changeQuantity(item, 1));
        HBox stepper = new HBox(10, minus, qty, plus);
        stepper.setAlignment(Pos.CENTER);

        Label sum = new Label(formatMoney(item.getTotal()));
        sum.getStyleClass().add("cart-row-sum");
        sum.setMinWidth(130);
        sum.setAlignment(Pos.CENTER_RIGHT);

        Button remove = new Button("✕");
        remove.getStyleClass().add("cart-remove-btn");
        remove.setOnAction(e -> {
            cart.remove(item);
            renderCartBar(false);
            renderCartScreen();
            setStatus(LocaleManager.t("cart.cleared"));
        });

        row.getChildren().addAll(text, stepper, sum, remove);
        return row;
    }

    private void changeQuantity(CartItem item, int delta) {
        int idx = cart.indexOf(item);
        if (idx < 0) {
            return;
        }
        int newQty = Math.max(1, item.getQuantity() + delta);
        if (newQty == item.getQuantity()) {
            return;
        }
        cart.set(idx, new CartItem(item.getService(), item.getAmount(), newQty));
        renderCartBar(false);
        renderCartScreen();
    }

    @FXML
    private void onClearCart() {
        cart.clear();
        renderCartBar(false);
        if (cartView.isVisible()) {
            renderCartScreen();
        }
        showToast(LocaleManager.t("cart.cleared"));
        setStatus(LocaleManager.t("cart.cleared"));
    }

    // ==================== ПОДТВЕРЖДЕНИЕ (ШАГ 1) ====================

    @FXML
    private void onCheckout() {
        if (cart.isEmpty()) {
            showToast(LocaleManager.t("pay.emptyCart.title"));
            return;
        }
        renderCheckoutScreen();
        showScreen(checkoutView);
    }

    @FXML
    private void onBackToCart() {
        renderCartScreen();
        showScreen(cartView);
    }

    private void renderCheckoutScreen() {
        payerErrorLabel.setVisible(false);
        payerErrorLabel.setManaged(false);
        checkoutItemsBox.getChildren().clear();

        for (CartItem item : cart) {
            HBox row = new HBox(14);
            row.getStyleClass().add("checkout-row");
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(item.getService().getName());
            name.getStyleClass().add("checkout-service-name");
            name.setWrapText(true);
            Label meta = new Label(item.getService().getDisplayProvider()
                    + " · " + item.getQuantity() + " × " + formatMoney(item.getAmount()));
            meta.getStyleClass().add("cart-row-meta");
            VBox text = new VBox(2, name, meta);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label sum = new Label(formatMoney(item.getTotal()));
            sum.getStyleClass().add("checkout-service-amount");
            row.getChildren().addAll(text, sum);
            checkoutItemsBox.getChildren().add(row);
        }

        HBox totalRow = new HBox(14);
        totalRow.getStyleClass().add("checkout-total-row");
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLabel = new Label(LocaleManager.t("pay.totalCreate"));
        totalLabel.getStyleClass().add("checkout-total-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalValue = new Label(formatMoney(cartTotal()));
        totalValue.getStyleClass().add("checkout-total-value");
        totalRow.getChildren().addAll(totalLabel, spacer, totalValue);
        checkoutItemsBox.getChildren().add(totalRow);
    }

    @FXML
    private void onCreatePayment() {
        String fio = payerFioField.getText() == null ? "" : payerFioField.getText().trim();
        if (fio.isBlank()) {
            payerErrorLabel.setText(LocaleManager.t("pay.err.fio"));
            payerErrorLabel.setVisible(true);
            payerErrorLabel.setManaged(true);
            return;
        }

        Map<String, List<CartItem>> groups = groupCartByProvider();
        int groupedItems = groups.values().stream().mapToInt(List::size).sum();
        String paymentProviderId = paymentProviderId();
        if (groups.isEmpty() || groupedItems != cart.size() || paymentProviderId == null) {
            showError(LocaleManager.t("pay.cantCreate.title"), LocaleManager.t("pay.cantCreate.body"));
            return;
        }

        String inn = payerInnField.getText() == null || payerInnField.getText().isBlank()
                ? null : payerInnField.getText().trim();
        createPayment(paymentProviderId, fio, inn);
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

    // ==================== СОЗДАНИЕ ПЛАТЕЖА / QR (ШАГ 2) ====================

    private void createPayment(String providerId, String fio, String inn) {
        createPaymentBtn.setDisable(true);
        setStatus(LocaleManager.t("pay.creating"));
        new Thread(() -> {
            try {
                Payment payment = ApiService.createPayment(cartTotal(), providerId, fio, inn);
                Platform.runLater(() -> {
                    createPaymentBtn.setDisable(false);
                    if (payment == null) {
                        showError(LocaleManager.t("pay.notCreated.title"), LocaleManager.t("pay.noData"));
                        return;
                    }
                    AppLogger.info("Создан общий платеж " + payment.getId() + ", сумма " + payment.getSum());
                    cart.clear();
                    renderCartBar(false);
                    payerFioField.clear();
                    payerInnField.clear();
                    setStatus(LocaleManager.t("pay.created"));
                    renderQrScreen(payment);
                    showScreen(qrView);
                });
            } catch (Exception e) {
                AppLogger.error("Ошибка создания платежа", e);
                Platform.runLater(() -> {
                    createPaymentBtn.setDisable(false);
                    setStatus(LocaleManager.t("pay.notCreated.title"));
                    showError(LocaleManager.t("pay.notCreated.title"),
                            LocaleManager.t("pay.notCreated.body") + "\n\n" + readableError(e));
                });
            }
        }).start();
    }

    private void renderQrScreen(Payment payment) {
        currentPayment = payment;

        // QR-код: ApiAB присылает PNG в base64.
        qrImageView.setImage(null);
        qrErrorLabel.setVisible(false);
        qrErrorLabel.setManaged(false);
        if (payment.getQrCode() != null && !payment.getQrCode().isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(payment.getQrCode());
                qrImageView.setImage(new Image(new ByteArrayInputStream(bytes)));
            } catch (IllegalArgumentException e) {
                showQrError(LocaleManager.t("pay.qrBroken"));
            }
        } else {
            showQrError(LocaleManager.t("pay.qrNoCode"));
        }

        // Реквизиты в белом «чеке».
        qrInfoBox.getChildren().clear();
        qrInfoBox.getChildren().add(buildQrRow(LocaleManager.t("pay.row.id"),
                nullToEmpty(payment.getId()), false));
        qrInfoBox.getChildren().add(buildQrRow(LocaleManager.t("pay.row.payer"),
                nullToEmpty(payment.getFio()), false));
        qrInfoBox.getChildren().add(buildQrRow(LocaleManager.t("pay.row.sum"),
                formatMoney(payment.getSum()), false));
        qrInfoBox.getChildren().add(buildQrRow(LocaleManager.t("pay.row.fee"),
                formatMoney(payment.getFee()), false));
        qrInfoBox.getChildren().add(buildQrRow(LocaleManager.t("pay.row.toPay"),
                formatMoney(payment.getTotal()), true));

        applyPayStatus(payment.getStatus());
        startPaymentTracking();
    }

    private void showQrError(String text) {
        qrErrorLabel.setText(text);
        qrErrorLabel.setVisible(true);
        qrErrorLabel.setManaged(true);
    }

    private HBox buildQrRow(String key, String value, boolean total) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add(total ? "qr-row-total-key" : "qr-row-key");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add(total ? "qr-row-total-val" : "qr-row-val");
        row.getChildren().addAll(keyLabel, spacer, valLabel);
        return row;
    }

    /** Пилюля статуса: цвет и подпись по статусу платежа, точка пульсирует. */
    private void applyPayStatus(String status) {
        payStatusPill.getStyleClass().setAll("payment-status-pill", statusClass(status));
        payStatusText.setText(statusLabel(status));
        if (pulseAnim == null) {
            pulseAnim = Fx.pulse(payStatusDot);
        }
    }

    /** Пока открыт экран QR — раз в 3 секунды спрашиваем у ApiAB статус платежа. */
    private void startPaymentTracking() {
        stopPaymentTracking();
        pulseAnim = Fx.pulse(payStatusDot);
        payPoll = new Timeline(new KeyFrame(Duration.seconds(3), e -> pollPaymentStatus()));
        payPoll.setCycleCount(Animation.INDEFINITE);
        payPoll.play();
    }

    private void stopPaymentTracking() {
        if (payPoll != null) {
            payPoll.stop();
            payPoll = null;
        }
        if (pulseAnim != null) {
            pulseAnim.stop();
            payStatusDot.setOpacity(1);
            pulseAnim = null;
        }
    }

    private void pollPaymentStatus() {
        Payment payment = currentPayment;
        if (payment == null || payment.getId() == null) {
            return;
        }
        new Thread(() -> {
            try {
                Payment fresh = ApiService.getPaymentById(payment.getId());
                if (fresh == null || fresh.getStatus() == null) {
                    return;
                }
                Platform.runLater(() -> {
                    if (!qrView.isVisible()) {
                        return;
                    }
                    payStatusPill.getStyleClass().setAll("payment-status-pill", statusClass(fresh.getStatus()));
                    payStatusText.setText(statusLabel(fresh.getStatus()));
                    if (!"processing".equals(fresh.getStatus()) && payPoll != null) {
                        payPoll.stop();
                    }
                });
            } catch (Exception e) {
                // Потеря связи при опросе не критична — просто попробуем в следующий тик.
                AppLogger.warn("Не удалось обновить статус платежа", e);
            }
        }).start();
    }

    @FXML
    private void onQrDone() {
        stopPaymentTracking();
        currentPayment = null;
        showHome();
    }

    // ==================== ТОСТ ====================

    private void showToast(String message) {
        if (toastAnim != null) {
            toastAnim.stop();
        }
        toastLabel.setText(message);
        toastAnim = Fx.toast(toastLabel);
    }

    // ==================== СЕССИЯ ====================

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
            // В киоск-режиме окно без рамки; задаём стиль до показа.
            WindowManager.configureDialog(stage);
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

    // ==================== ХЕЛПЕРЫ ====================

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

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP) + " " + LocaleManager.t("money.suffix");
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
