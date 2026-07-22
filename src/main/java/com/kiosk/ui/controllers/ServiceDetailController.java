package com.kiosk.ui.controllers;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.util.LocaleManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceDetailController {

    @FXML private Label serviceNameLabel, categoryLabel, providerLabel, accountLabel, commissionLabel;
    @FXML private Label detailErrorLabel;
    @FXML private TextField amountField, quantityField;
    @FXML private Button favoriteBtn;

    private Stage stage;
    private Runnable onClose;
    private ProviderService service;
    private CartAddHandler onAddToCart;
    private static final Set<String> favorites = new HashSet<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /** В новом дизайне шторка живёт внутри сцены — вместо Stage передаётся колбэк закрытия. */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setService(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        // Услуга передается сюда из карточки на главном экране.
        this.service = service;
        serviceNameLabel.setText(service.getName());

        if (service.hasFixedPrice()) {
            amountField.setText(service.getPrice().toPlainString());
            amountField.setEditable(false);
            amountField.setFocusTraversable(false);
        }

        // В новом ApiAB категория может прийти названием, а в старых данных только id.
        String catName = service.getCategoryName() != null ? service.getCategoryName() : categories.stream()
                .filter(c -> c.getId().equals(service.getCategoryId()))
                .findFirst()
                .map(CategoryService::getName)
                .orElse("—");
        categoryLabel.setText(catName);

        // Провайдера тоже сначала берем из DTO, а если его нет, ищем в списке по provId.
        String provName = service.getProviderName() != null ? service.getProviderName() : providers.stream()
                .filter(p -> p.getId().equals(service.getProvId()))
                .findFirst()
                .map(Provider::toString)
                .orElse("—");
        providerLabel.setText(provName);

        accountLabel.setText(service.getDisplayAccount());

        commissionLabel.setText(service.getCommission() != null
                ? service.getCommission().getDisplayText()
                : LocaleManager.t("detail.commissionDefault"));

        // Кнопка избранного
        updateFavoriteBtn(service.getId());
        favoriteBtn.setOnAction(e -> {
            if (favorites.contains(service.getId())) {
                favorites.remove(service.getId());
            } else {
                favorites.add(service.getId());
            }
            updateFavoriteBtn(service.getId());
        });
    }

    public void setOnAddToCart(CartAddHandler onAddToCart) {
        this.onAddToCart = onAddToCart;
    }

    private void updateFavoriteBtn(String serviceId) {
        // Кнопка-звёздочка, как в макете: контур — не в избранном, заливка — в избранном.
        if (favorites.contains(serviceId)) {
            favoriteBtn.setText("★");
            favoriteBtn.getStyleClass().add("btn-favorite-active");
        } else {
            favoriteBtn.setText("☆");
            favoriteBtn.getStyleClass().removeAll("btn-favorite-active");
        }
    }

    @FXML
    private void onFavorite() {
        // Обрабатывается в setService через setOnAction
    }

    @FXML
    private void onAmountChip(javafx.event.ActionEvent e) {
        // Быстрые суммы недоступны, если у услуги фиксированная цена.
        if (!amountField.isEditable()) {
            return;
        }
        amountField.setText(((Button) e.getSource()).getText());
        hideDetailError();
    }

    @FXML
    private void onQtyMinus() {
        changeQuantity(-1);
    }

    @FXML
    private void onQtyPlus() {
        changeQuantity(1);
    }

    private void changeQuantity(int delta) {
        int current;
        try {
            current = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            current = 1;
        }
        quantityField.setText(String.valueOf(Math.max(1, current + delta)));
        hideDetailError();
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        } else if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void onAddToCart() {
        try {
            // Сумма вводится за одну единицу услуги.
            BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showDetailError(LocaleManager.t("detail.err.amount"));
                return;
            }
            // Количество умножится на сумму уже в CartItem.
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                showDetailError(LocaleManager.t("detail.err.quantity"));
                return;
            }
            if (onAddToCart != null && service != null) {
                onAddToCart.add(service, amount, quantity);
            }
            onClose();
        } catch (NumberFormatException e) {
            showDetailError(LocaleManager.t("detail.err.format"));
        }
    }

    private void showDetailError(String text) {
        detailErrorLabel.setText(text);
        detailErrorLabel.setVisible(true);
        detailErrorLabel.setManaged(true);
    }

    private void hideDetailError() {
        detailErrorLabel.setVisible(false);
        detailErrorLabel.setManaged(false);
    }

    public static Set<String> getFavorites() {
        return favorites;
    }

    @FunctionalInterface
    public interface CartAddHandler {
        void add(ProviderService service, BigDecimal amount, int quantity);
    }
}
