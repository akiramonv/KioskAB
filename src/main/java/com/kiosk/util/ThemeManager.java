package com.kiosk.util;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    // По умолчанию тёмная тема — канонический вид макета «Киоск — новый дизайн».
    private static boolean darkMode = true;

    private ThemeManager() {}

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void setDarkMode(boolean enabled) {
        darkMode = enabled;
    }

    public static void toggle() {
        darkMode = !darkMode;
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        Parent root = scene.getRoot();
        root.getStyleClass().remove("dark");
        if (darkMode) {
            root.getStyleClass().add("dark");
        }
    }
}
