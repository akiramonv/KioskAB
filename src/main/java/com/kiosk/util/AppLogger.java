package com.kiosk.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("kiosk-app");
    private static boolean configured = false;

    private AppLogger() {}

    public static synchronized void configure() {
        if (configured) {
            return;
        }
        try {
            Path logsDir = Path.of("logs");
            Files.createDirectories(logsDir);
            FileHandler handler = new FileHandler(logsDir.resolve("kiosk.log").toString(), true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.INFO);
            configured = true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Не удалось настроить локальный лог", e);
        }
    }

    public static void info(String message) {
        configure();
        LOGGER.info(message);
    }

    public static void warn(String message, Throwable throwable) {
        configure();
        LOGGER.log(Level.WARNING, message, throwable);
    }

    public static void error(String message, Throwable throwable) {
        configure();
        LOGGER.log(Level.SEVERE, message, throwable);
    }
}
