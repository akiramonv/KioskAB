package com.kiosk.ui.components;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.ui.controllers.ServiceDetailController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Карточка услуги — компонент для отображения в сетке.
 * Макет «Киоск — новый дизайн»: провайдер и избранное сверху,
 * крупное название посередине, категория и комиссия снизу.
 */
public class ServiceCard extends VBox {

    public ServiceCard(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        getStyleClass().add("service-card");
        setPrefWidth(340);
        setMinWidth(300);
        setMaxWidth(380);
        setPadding(new Insets(18));
        setSpacing(10);
        setAlignment(Pos.TOP_LEFT);

        // Провайдер
        String provName = service.getProviderName() != null ? service.getProviderName() : providers.stream()
                .filter(p -> p.getId().equals(service.getProvId()))
                .findFirst()
                .map(Provider::toString)
                .orElse("—");
        Label provLabel = new Label(provName);
        provLabel.getStyleClass().add("card-provider");

        // Верхняя строка: провайдер слева, признак избранного справа
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topRow.getChildren().addAll(provLabel, topSpacer);
        if (ServiceDetailController.getFavorites().contains(service.getId())) {
            Label favLabel = new Label("★");
            favLabel.getStyleClass().add("card-favorite");
            topRow.getChildren().add(favLabel);
        }

        // Название услуги
        Label nameLabel = new Label(service.getName());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(Region.USE_PREF_SIZE);

        // Нижняя строка: категория слева, цена справа
        String catName = service.getCategoryName() != null ? service.getCategoryName() : categories.stream()
                .filter(c -> c.getId().equals(service.getCategoryId()))
                .findFirst()
                .map(CategoryService::getName)
                .orElse("—");
        Label catLabel = new Label("📁 " + catName);
        catLabel.getStyleClass().add("card-category");

        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        bottomRow.getChildren().addAll(catLabel, bottomSpacer);

        // Цена услуги (если задана в ApiAB)
        String displayPrice = service.getDisplayPrice();
        if (displayPrice != null) {
            Label priceLabel = new Label(displayPrice);
            priceLabel.getStyleClass().add("card-price");
            bottomRow.getChildren().add(priceLabel);
        }

        getChildren().addAll(topRow, nameLabel, bottomRow);

        // Hover-курсор
        setStyle("-fx-cursor: hand;");
    }
}
