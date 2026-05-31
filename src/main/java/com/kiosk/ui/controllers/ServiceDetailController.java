package com.kiosk.ui.controllers;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceDetailController {

    @FXML private Label serviceNameLabel, categoryLabel, providerLabel, accountLabel, commissionLabel;
    @FXML private Button favoriteBtn;

    private Stage stage;
    private static final Set<String> favorites = new HashSet<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setService(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        serviceNameLabel.setText(service.getName());

        // Категория
        String catName = categories.stream()
                .filter(c -> c.getId().equals(service.getCategoryId()))
                .findFirst()
                .map(CategoryService::getName)
                .orElse("—");
        categoryLabel.setText(catName);

        // Провайдер
        String provName = providers.stream()
                .filter(p -> p.getId().equals(service.getProvId()))
                .findFirst()
                .map(Provider::toString)
                .orElse("—");
        providerLabel.setText(provName);

        // Счёт
        accountLabel.setText(service.getAccountId() != null ? service.getAccountId() : "—");

        // Комиссия
        commissionLabel.setText(service.getCommId() != null
                ? "Индивидуальная (ID: " + service.getCommId() + ")"
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

    public static Set<String> getFavorites() {
        return favorites;
    }
}
