package com.kiosk.util;

import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Управляет режимом отображения окон киоска.
 * <p>
 * По умолчанию все окна открываются развёрнутыми на весь экран («полный экран»).
 * Дополнительно доступен <b>безграничный (киоск) режим</b>: главное окно
 * переводится в настоящий полноэкранный режим без системной рамки, поэтому
 * кнопка закрытия не видна и клиент не может случайно закрыть приложение.
 * Выход по {@code ESC} в этом режиме отключён, чтобы случайное нажатие не
 * вывело терминал из киоска — выключить режим можно только через меню настроек.
 * <p>
 * Модальные окна (детали услуги, панель администратора) в киоск-режиме
 * показываются без рамки ({@link StageStyle#UNDECORATED}), сохраняя свой размер;
 * закрыть их по-прежнему можно встроенной кнопкой внутри окна.
 */
public final class WindowManager {

    // Безграничный режим по умолчанию выключен — оператор включает его перед сменой.
    private static boolean kioskMode = false;

    private WindowManager() {}

    public static boolean isKioskMode() {
        return kioskMode;
    }

    public static void setKioskMode(boolean enabled) {
        kioskMode = enabled;
        // Уже открытые НЕмодальные окна (главное окно) переключаем сразу.
        refreshOpenPrimaryWindows();
    }

    public static void toggle() {
        setKioskMode(!kioskMode);
    }

    /**
     * Настраивает главное окно приложения: разворачивает на весь экран, а в
     * киоск-режиме переводит в полноэкранный режим без рамки и кнопки закрытия.
     * Можно вызывать как до, так и после {@link Stage#show()}.
     */
    public static void configure(Stage stage) {
        if (stage == null) {
            return;
        }
        if (kioskMode) {
            stage.setMaximized(true);
            // Полный экран без рамки прячет кнопку закрытия; отключаем выход по ESC,
            // чтобы клиент не вышел из режима киоска случайным нажатием.
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);
        } else {
            stage.setFullScreen(false);
            stage.setMaximized(true);
        }
    }

    /**
     * Настраивает модальное окно. Вызывать <b>до</b> первого показа окна, потому
     * что стиль рамки в JavaFX задаётся только до {@link Stage#show()}.
     * В киоск-режиме прячет системную рамку с кнопкой закрытия, размер окна
     * остаётся прежним (его задаёт вызывающая сторона).
     */
    public static void configureDialog(Stage stage) {
        if (stage == null) {
            return;
        }
        if (kioskMode) {
            stage.initStyle(StageStyle.UNDECORATED);
        }
    }

    private static void refreshOpenPrimaryWindows() {
        for (Window window : Window.getWindows()) {
            // Модальным окнам стиль рамки на лету сменить нельзя, поэтому трогаем
            // только обычные (немодальные) окна — это главное окно терминала.
            if (window instanceof Stage stage && stage.getModality() == Modality.NONE) {
                configure(stage);
            }
        }
    }
}
