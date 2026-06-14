package com.kiosk.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Локализация приложения. Поддерживаются три языка: русский, английский и
 * кыргызский. Строки лежат в {@code /i18n/messages_<lang>.properties} (UTF-8).
 * <p>
 * Бандл загружается вручную через {@link PropertyResourceBundle} с UTF-8 Reader,
 * чтобы не зависеть от модульных особенностей {@link ResourceBundle#getBundle}.
 * Тот же бандл отдаётся в {@code FXMLLoader.setResources}, поэтому в FXML можно
 * использовать {@code %ключ}.
 */
public final class LocaleManager {

    public static final String RU = "ru";
    public static final String EN = "en";
    public static final String KY = "ky";

    private static String language = RU;
    private static ResourceBundle bundle = load(RU);

    private LocaleManager() {}

    public static String getLanguage() {
        return language;
    }

    public static Locale getLocale() {
        return switch (language) {
            case EN -> Locale.ENGLISH;
            case KY -> new Locale("ky");
            default -> new Locale("ru");
        };
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static void setLanguage(String lang) {
        if (lang == null || lang.equals(language)) {
            return;
        }
        language = switch (lang) {
            case EN, KY, RU -> lang;
            default -> RU;
        };
        bundle = load(language);
    }

    /** Перевод по ключу; если ключа нет — возвращает сам ключ (чтобы было видно пропуск). */
    public static String t(String key) {
        if (key == null) {
            return "";
        }
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    /** Перевод с подстановкой {@code {0} {1} ...} (без MessageFormat, чтобы не возиться с кавычками). */
    public static String t(String key, Object... args) {
        String value = t(key);
        for (int i = 0; i < args.length; i++) {
            value = value.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return value;
    }

    private static ResourceBundle load(String lang) {
        String path = "/i18n/messages_" + lang + ".properties";
        try (InputStream in = LocaleManager.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Не найден файл локализации: " + path);
            }
            return new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить локализацию " + lang, e);
        }
    }
}
