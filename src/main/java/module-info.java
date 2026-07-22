module com.kiosk {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires java.logging;
    // Нужен демо-режиму для локальной генерации QR-кода (BufferedImage/ImageIO).
    requires java.desktop;

    opens com.kiosk to javafx.fxml;
    opens com.kiosk.ui.controllers to javafx.fxml;
    opens com.kiosk.model to com.fasterxml.jackson.databind;

    exports com.kiosk;
    exports com.kiosk.ui.controllers;
    exports com.kiosk.ui.components;
    exports com.kiosk.model;
    exports com.kiosk.service;
    exports com.kiosk.api;
    exports com.kiosk.util;
}
