package com.kiosk.ui.controllers;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
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
    private ProviderService service;
    private CartAddHandler onAddToCart;
    private static final Set<String> favorites = new HashSet<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setService(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        // Услуга передается сюда из карточки на главном экране.
        this.service = service;
        serviceNameLabel.setText(service.getName());

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
                : "По умолчанию провайдера");

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
        if (favorites.contains(serviceId)) {
            favoriteBtn.setText("★ В избранном");
            favoriteBtn.getStyleClass().add("btn-favorite-active");
        } else {
            favoriteBtn.setText("☆ В избранное");
            favoriteBtn.getStyleClass().removeAll("btn-favorite-active");
        }
    }

    @FXML
    private void onFavorite() {
        // Обрабатывается в setService через setOnAction
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }

    @FXML
    private void onAddToCart() {
        try {
            // Сумма вводится за одну единицу услуги.
            BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showDetailError("Введите сумму больше 0.");
                return;
            }
            // Количество умножится на сумму уже в CartItem.
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                showDetailError("Количество должно быть больше 0.");
                return;
            }
            if (onAddToCart != null && service != null) {
                onAddToCart.add(service, amount, quantity);
            }
            if (stage != null) stage.close();
        } catch (NumberFormatException e) {
            showDetailError("Проверьте сумму и количество. Например: сумма 250.00, количество 2.");
        }
    }

    private void showDetailError(String text) {
        detailErrorLabel.setText(text);
        detailErrorLabel.setVisible(true);
    }

    public static Set<String> getFavorites() {
        return favorites;
    }

    @FunctionalInterface
    public interface CartAddHandler {
        void add(ProviderService service, BigDecimal amount, int quantity);
    }
}
