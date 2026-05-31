package com.kiosk.ui.components;

import com.kiosk.model.CategoryService;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.ui.controllers.ServiceDetailController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Карточка услуги — компонент для отображения в сетке.
 */
public class ServiceCard extends VBox {

    private static final String[] CARD_ICONS = {"💳", "🏦", "📱", "🏠", "🚗", "⚡", "💧", "📡", "🎓", "🏥"};

    public ServiceCard(ProviderService service, List<Provider> providers, List<CategoryService> categories) {
        getStyleClass().add("service-card");
        setPrefWidth(200);
        setMinWidth(180);
        setMaxWidth(220);
        setPadding(new Insets(16));
        setSpacing(8);
        setAlignment(Pos.TOP_LEFT);

        // Иконка — выбирается по хэшу имени
        String icon = CARD_ICONS[Math.abs(service.getName().hashCode()) % CARD_ICONS.length];
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("card-icon");

        // Название услуги
        Label nameLabel = new Label(service.getName());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true);

        // Провайдер
        String provName = providers.stream()
                .filter(p -> p.getId().equals(service.getProvId()))
                .findFirst()
                .map(Provider::toString)
                .orElse("—");
        Label provLabel = new Label(provName);
        provLabel.getStyleClass().add("card-provider");

        // Категория
        String catName = categories.stream()
                .filter(c -> c.getId().equals(service.getCategoryId()))
                .findFirst()
                .map(CategoryService::getName)
                .orElse("—");
        Label catLabel = new Label("📁 " + catName);
        catLabel.getStyleClass().add("card-category");

        // Признак избранного
        if (ServiceDetailController.getFavorites().contains(service.getId())) {
            Label favLabel = new Label("★");
            favLabel.getStyleClass().add("card-favorite");
            getChildren().addAll(iconLabel, nameLabel, provLabel, catLabel, favLabel);
        } else {
            getChildren().addAll(iconLabel, nameLabel, provLabel, catLabel);
        }

        // Hover-курсор
        setStyle("-fx-cursor: hand;");
    }
}
