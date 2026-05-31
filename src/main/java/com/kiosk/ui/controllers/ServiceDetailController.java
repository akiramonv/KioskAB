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
import java.util.function.BiConsumer;

public class ServiceDetailController {

    @FXML private Label serviceNameLabel, categoryLabel, providerLabel, accountLabel, commissionLabel;
    @FXML private Label detailErrorLabel;
    @FXML private TextField amountField;
    @FXML private Button favoriteBtn;

    private Stage stage;
    private ProviderService service;
    private BiConsumer<ProviderService, BigDecimal> onAddToCart;
    private static final Set<String> favorites = new HashSet<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setService(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        this.service = service;
        serviceNameLabel.setText(service.getName());

        String catName = service.getCategoryName() != null ? service.getCategoryName() : categories.stream()
                .filter(c -> c.getId().equals(service.getCategoryId()))
                .findFirst()
                .map(CategoryService::getName)
                .orElse("—");
        categoryLabel.setText(catName);

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

    public void setOnAddToCart(BiConsumer<ProviderService, BigDecimal> onAddToCart) {
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
            BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showDetailError("Введите сумму больше 0.");
                return;
            }
            if (onAddToCart != null && service != null) {
                onAddToCart.accept(service, amount);
            }
            if (stage != null) stage.close();
        } catch (NumberFormatException e) {
            showDetailError("Сумма должна быть числом, например 250.00.");
        }
    }

    private void showDetailError(String text) {
        detailErrorLabel.setText(text);
        detailErrorLabel.setVisible(true);
    }

    public static Set<String> getFavorites() {
        return favorites;
    }
}
