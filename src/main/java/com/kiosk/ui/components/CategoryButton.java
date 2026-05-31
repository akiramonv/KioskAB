package com.kiosk.ui.components;

import com.kiosk.model.CategoryService;
import javafx.scene.control.Button;

/**
 * Кнопка категории в боковой панели.
 */
public class CategoryButton extends Button {

    private static final String[] CAT_ICONS = {"📋", "🏷", "📦", "🎯", "🔧", "📊", "🌐", "🎓", "🏥", "🚀"};

    public CategoryButton(CategoryService category) {
        String icon = CAT_ICONS[Math.abs(category.getName().hashCode()) % CAT_ICONS.length];
        setText(icon + " " + category.getName());
        getStyleClass().add("category-btn");
    }
}
