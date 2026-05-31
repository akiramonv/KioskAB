package com.kiosk.ui.controllers;

import com.kiosk.model.*;
import com.kiosk.service.ApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.*;

public class AdminController {

    @FXML private TableView<Object> adminTable;
    @FXML private TextField adminSearchField;
    @FXML private Button addBtn, editBtn, deleteBtn;
    @FXML private Label adminStatus;

    private Stage stage;
    private String currentSection = "";
    private List<?> currentData = new ArrayList<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML public void initialize() {
        adminTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean selected = newVal != null;
            editBtn.setDisable(!selected);
            deleteBtn.setDisable(!selected);
        });
    }

    // ==================== НАВИГАЦИЯ ====================

    @FXML private void showUsers() { loadUsers(); }
    @FXML private void showServices() { loadServices(); }
    @FXML private void showCategories() { loadCategories(); }
    @FXML private void showProviders() { loadProviders(); }
    @FXML private void showSpecializations() { loadSpecializations(); }

    // ==================== ЗАГРУЗКА ДАННЫХ ====================

    private void loadUsers() {
        currentSection = "users";
        setStatus("Загрузка пользователей...");
        new Thread(() -> {
            try {
                List<User> users = ApiService.getAllUsers();
                Platform.runLater(() -> {
                    currentData = users;
                    setupUsersTable(users);
                    setStatus("Загружено: " + users.size() + " пользователей");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void loadServices() {
        currentSection = "services";
        setStatus("Загрузка услуг...");
        new Thread(() -> {
            try {
                List<ProviderService> services = ApiService.getAllServices();
                Platform.runLater(() -> {
                    currentData = services;
                    setupServicesTable(services);
                    setStatus("Загружено: " + services.size() + " услуг");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void loadCategories() {
        currentSection = "categories";
        setStatus("Загрузка категорий...");
        new Thread(() -> {
            try {
                List<CategoryService> categories = ApiService.getAllCategories();
                Platform.runLater(() -> {
                    currentData = categories;
                    setupCategoriesTable(categories);
                    setStatus("Загружено: " + categories.size() + " категорий");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void loadProviders() {
        currentSection = "providers";
        setStatus("Загрузка провайдеров...");
        new Thread(() -> {
            try {
                List<Provider> providers = ApiService.getAllProviders();
                Platform.runLater(() -> {
                    currentData = providers;
                    setupProvidersTable(providers);
                    setStatus("Загружено: " + providers.size() + " провайдеров");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void loadSpecializations() {
        currentSection = "specializations";
        setStatus("Загрузка специализаций...");
        new Thread(() -> {
            try {
                List<Specialization> specs = ApiService.getAllSpecializations();
                Platform.runLater(() -> {
                    currentData = specs;
                    setupSpecializationsTable(specs);
                    setStatus("Загружено: " + specs.size() + " специализаций");
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    // ==================== НАСТРОЙКА ТАБЛИЦ ====================

    @SuppressWarnings("unchecked")
    private void setupUsersTable(List<User> users) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 280);
        TableColumn<Object, String> colName = col("Имя", 180);
        TableColumn<Object, String> colEmail = col("Email", 220);
        TableColumn<Object, String> colProv = col("Провайдер ID", 280);

        colId.setCellValueFactory(d -> new SimpleStringProperty(((User) d.getValue()).getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(((User) d.getValue()).getName()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(((User) d.getValue()).getEmail()));
        colProv.setCellValueFactory(d -> new SimpleStringProperty(((User) d.getValue()).getProvId()));

        adminTable.getColumns().addAll(colId, colName, colEmail, colProv);
        adminTable.setItems(FXCollections.observableArrayList(users));
    }

    @SuppressWarnings("unchecked")
    private void setupServicesTable(List<ProviderService> services) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 280);
        TableColumn<Object, String> colName = col("Название", 220);
        TableColumn<Object, String> colCat = col("Категория ID", 280);
        TableColumn<Object, String> colProv = col("Провайдер ID", 280);

        colId.setCellValueFactory(d -> new SimpleStringProperty(((ProviderService) d.getValue()).getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(((ProviderService) d.getValue()).getName()));
        colCat.setCellValueFactory(d -> new SimpleStringProperty(((ProviderService) d.getValue()).getCategoryId()));
        colProv.setCellValueFactory(d -> new SimpleStringProperty(((ProviderService) d.getValue()).getProvId()));

        adminTable.getColumns().addAll(colId, colName, colCat, colProv);
        adminTable.setItems(FXCollections.observableArrayList(services));
    }

    @SuppressWarnings("unchecked")
    private void setupCategoriesTable(List<CategoryService> categories) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 280);
        TableColumn<Object, String> colName = col("Название", 220);
        TableColumn<Object, String> colParent = col("Родитель ID", 280);
        TableColumn<Object, String> colProv = col("Провайдер ID", 280);

        colId.setCellValueFactory(d -> new SimpleStringProperty(((CategoryService) d.getValue()).getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(((CategoryService) d.getValue()).getName()));
        colParent.setCellValueFactory(d -> new SimpleStringProperty(Optional.ofNullable(((CategoryService) d.getValue()).getPrntCategory()).orElse("—")));
        colProv.setCellValueFactory(d -> new SimpleStringProperty(((CategoryService) d.getValue()).getProvId()));

        adminTable.getColumns().addAll(colId, colName, colParent, colProv);
        adminTable.setItems(FXCollections.observableArrayList(categories));
    }

    @SuppressWarnings("unchecked")
    private void setupProvidersTable(List<Provider> providers) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 280);
        TableColumn<Object, String> colFull = col("Полное название", 240);
        TableColumn<Object, String> colShort = col("Краткое", 160);
        TableColumn<Object, String> colLvl = col("Уровень комиссии", 180);

        colId.setCellValueFactory(d -> new SimpleStringProperty(((Provider) d.getValue()).getId()));
        colFull.setCellValueFactory(d -> new SimpleStringProperty(((Provider) d.getValue()).getFullName()));
        colShort.setCellValueFactory(d -> new SimpleStringProperty(((Provider) d.getValue()).getShortName()));
        colLvl.setCellValueFactory(d -> new SimpleStringProperty(((Provider) d.getValue()).getCommLvl()));

        adminTable.getColumns().addAll(colId, colFull, colShort, colLvl);
        adminTable.setItems(FXCollections.observableArrayList(providers));
    }

    @SuppressWarnings("unchecked")
    private void setupSpecializationsTable(List<Specialization> specs) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 280);
        TableColumn<Object, String> colName = col("Название", 240);
        TableColumn<Object, String> colProv = col("Провайдер ID", 280);

        colId.setCellValueFactory(d -> new SimpleStringProperty(((Specialization) d.getValue()).getId()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(((Specialization) d.getValue()).getName()));
        colProv.setCellValueFactory(d -> new SimpleStringProperty(((Specialization) d.getValue()).getProvId()));

        adminTable.getColumns().addAll(colId, colName, colProv);
        adminTable.setItems(FXCollections.observableArrayList(specs));
    }

    // ==================== CRUD ====================

    @FXML
    private void onAdd() {
        showInputDialog("Добавить запись", null);
    }

    @FXML
    private void onEdit() {
        Object selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInputDialog("Редактировать", selected);
        }
    }

    @FXML
    private void onDelete() {
        Object selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление");
        confirm.setHeaderText("Удалить запись?");
        confirm.setContentText("Это действие необратимо.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                performDelete(selected);
            }
        });
    }

    private void performDelete(Object item) {
        new Thread(() -> {
            try {
                boolean success = false;
                if (item instanceof User u) success = ApiService.deleteUser(u.getId());
                else if (item instanceof ProviderService s) success = ApiService.deleteService(s.getId());
                else if (item instanceof CategoryService c) success = ApiService.deleteCategory(c.getId());
                else if (item instanceof Provider p) success = ApiService.deleteProvider(p.getId());

                boolean finalSuccess = success;
                Platform.runLater(() -> {
                    if (finalSuccess) {
                        setStatus("Запись удалена успешно");
                        refreshCurrentSection();
                    } else {
                        setStatus("Ошибка удаления");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void showInputDialog(String title, Object existingItem) {
        // Простая форма для добавления/редактирования
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        var grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Название");

        if (existingItem instanceof ProviderService s) nameField.setText(s.getName());
        else if (existingItem instanceof CategoryService c) nameField.setText(c.getName());
        else if (existingItem instanceof Provider p) nameField.setText(p.getFullName());
        else if (existingItem instanceof Specialization sp) nameField.setText(sp.getName());
        else if (existingItem instanceof User u) nameField.setText(u.getName());

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return Map.of("name", nameField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> {
            if (data.isEmpty() || data.get("name").isEmpty()) return;
            performSave(existingItem, data);
        });
    }

    private void performSave(Object existingItem, Map<String, String> data) {
        new Thread(() -> {
            try {
                Map<String, Object> body = new HashMap<>(data);
                if (existingItem == null) {
                    // Создать
                    if ("services".equals(currentSection)) ApiService.createService(body);
                    else if ("categories".equals(currentSection)) ApiService.createCategory(body);
                    else if ("providers".equals(currentSection)) ApiService.createProvider(body);
                } else {
                    // Обновить
                    if (existingItem instanceof ProviderService s) ApiService.updateService(s.getId(), body);
                    else if (existingItem instanceof CategoryService c) ApiService.updateCategory(c.getId(), body);
                    else if (existingItem instanceof Provider p) ApiService.updateProvider(p.getId(), body);
                    else if (existingItem instanceof User u) ApiService.updateUser(u.getId(), body);
                }
                Platform.runLater(() -> {
                    setStatus("Сохранено");
                    refreshCurrentSection();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка сохранения: " + e.getMessage()));
            }
        }).start();
    }

    private void refreshCurrentSection() {
        switch (currentSection) {
            case "users" -> loadUsers();
            case "services" -> loadServices();
            case "categories" -> loadCategories();
            case "providers" -> loadProviders();
            case "specializations" -> loadSpecializations();
        }
    }

    @FXML
    private void onAdminSearch() {
        String q = adminSearchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            adminTable.setItems(FXCollections.observableArrayList(currentData));
            return;
        }
        List<Object> filtered = currentData.stream()
                .filter(item -> getItemName(item).toLowerCase().contains(q))
                .collect(java.util.stream.Collectors.toList());
        adminTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private String getItemName(Object item) {
        if (item instanceof User u) return u.getName() + " " + u.getEmail();
        if (item instanceof ProviderService s) return s.getName();
        if (item instanceof CategoryService c) return c.getName();
        if (item instanceof Provider p) return p.getFullName() + " " + p.getShortName();
        if (item instanceof Specialization sp) return sp.getName();
        return "";
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }

    private void setStatus(String text) {
        adminStatus.setText(text);
    }

    private <T> TableColumn<Object, T> col(String title, double width) {
        TableColumn<Object, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        return col;
    }
}
