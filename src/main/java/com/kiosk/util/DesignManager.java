package com.kiosk.util;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Управляет выбором визуального оформления приложения.
 * <p>
 * Поддерживаются две темы оформления: новый дизайн (PAYTERMINAL) и старый.
 * Переключение реализовано подменой подключённой таблицы стилей у сцены или
 * диалоговой панели, поэтому интерфейс перерисовывается без перезагрузки FXML.
 * <p>
 * Тёмная тема нового дизайна подключается отдельным файлом без префикса
 * {@code .dark} — так стили достают и попапы (контекст-меню, выпадающие списки),
 * которые не наследуют класс корня сцены.
 */
public final class DesignManager {

    private static final String OLD_CSS = "/css/styles.css";
    private static final String NEW_CSS = "/css/styles-v2.css";
    private static final String NEW_DARK_CSS = "/css/styles-v2-dark.css";

    // По умолчанию показываем новый дизайн, старый доступен через переключатель.
    private static boolean newDesign = true;

    private DesignManager() {}

    public static boolean isNewDesign() {
        return newDesign;
    }

    public static void setNewDesign(boolean enabled) {
        newDesign = enabled;
    }

    public static void toggle() {
        newDesign = !newDesign;
    }

    /** Список таблиц стилей для текущего дизайна и темы (порядок важен). */
    public static List<String> stylesheets() {
        List<String> sheets = new ArrayList<>();
        if (newDesign) {
            sheets.add(resource(NEW_CSS));
            if (ThemeManager.isDarkMode()) {
                sheets.add(resource(NEW_DARK_CSS));
            }
        } else {
            sheets.add(resource(OLD_CSS));
        }
        return sheets;
    }

    private static String resource(String path) {
        return Objects.requireNonNull(DesignManager.class.getResource(path),
                "Не найдена таблица стилей: " + path).toExternalForm();
    }

    /** Подключает к сцене активный дизайн и текущую тему (светлая/тёмная). */
    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(DesignManager::isManagedStylesheet);
        scene.getStylesheets().addAll(stylesheets());
        ThemeManager.apply(scene);
    }

    /** Подключает активный дизайн к диалоговой панели. */
    public static void apply(DialogPane pane) {
        if (pane == null) {
            return;
        }
        pane.getStylesheets().removeIf(DesignManager::isManagedStylesheet);
        pane.getStylesheets().addAll(stylesheets());
    }

    private static boolean isManagedStylesheet(String url) {
        return url.endsWith("styles.css")
                || url.endsWith("styles-v2.css")
                || url.endsWith("styles-v2-dark.css");
    }
}
