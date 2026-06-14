package com.kiosk.ui.controllers;

import com.kiosk.model.*;
import com.kiosk.service.ApiService;
import com.kiosk.util.AppLogger;
import com.kiosk.util.LocaleManager;
import com.kiosk.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private TableView<Object> adminTable;
    @FXML private TextField adminSearchField, minAmountField, maxAmountField;
    @FXML private ComboBox<String> paymentStatusFilter;
    @FXML private DatePicker dateFromPicker, dateToPicker;
    @FXML private HBox paymentFilters;
    @FXML private Button addBtn, editBtn, deleteBtn;
    @FXML private Label adminStatus;

    private Stage stage;
    private String currentSection = "";
    private List<Object> currentData = new ArrayList<>();

    private List<Provider> providers = new ArrayList<>();
    private List<CategoryService> categories = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private List<Commission> commissions = new ArrayList<>();
    private List<CommissionLevel> commissionLevels = new ArrayList<>();
    private List<Role> roles = new ArrayList<>();
    private List<Specialization> specializations = new ArrayList<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        paymentStatusFilter.setItems(FXCollections.observableArrayList("Все", "processing", "success", "cancel"));
        paymentStatusFilter.getSelectionModel().select("Все");
        adminTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            editBtn.setDisable(!hasSelection || currentSection.isBlank());
            deleteBtn.setDisable(!hasSelection || currentSection.isBlank()
                    || ("providers".equals(currentSection) && isOrganizationBound()));
        });
    }

    // ==================== НАВИГАЦИЯ ====================

    @FXML private void showUsers() { loadUsers(); }
    @FXML private void showServices() { loadServices(); }
    @FXML private void showCategories() { loadCategories(); }
    @FXML private void showProviders() { loadProviders(); }
    @FXML private void showSpecializations() { loadSpecializations(); }
    @FXML private void showPayments() { loadPayments(); }

    // ==================== ЗАГРУЗКА ДАННЫХ ====================

    private void prepareSection(String section, String status) {
        currentSection = section;
        setStatus(status);
        adminSearchField.clear();
        // Фильтры суммы/даты нужны только для раздела платежей.
        boolean payments = "payments".equals(section);
        paymentFilters.setVisible(payments);
        paymentFilters.setManaged(payments);
        // Обычный админ видит своего провайдера, но не может создать новую организацию.
        addBtn.setDisable(payments || ("providers".equals(section) && isOrganizationBound()));
        editBtn.setText(payments ? LocaleManager.t("admin.statusBtn") : LocaleManager.t("admin.edit"));
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);
    }

    private void loadUsers() {
        prepareSection("users", "Загрузка пользователей...");
        new Thread(() -> {
            try {
                // Админ организации видит пользователей только своего провайдера.
                List<User> users = isOrganizationBound() && hasCurrentProvider()
                        ? ApiService.getUsersByProvider(currentProviderId())
                        : isOrganizationBound() ? List.of() : ApiService.getAllUsers();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(users);
                    setupUsersTable(users);
                    setStatus("Загружено: " + users.size() + " пользователей");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void loadServices() {
        prepareSection("services", "Загрузка услуг...");
        new Thread(() -> {
            try {
                // Услуги для обычного админа тянутся по providerId из сессии.
                List<ProviderService> services = isOrganizationBound() && hasCurrentProvider()
                        ? ApiService.getServicesByProvider(currentProviderId())
                        : isOrganizationBound() ? List.of() : ApiService.getAllServices();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(services);
                    setupServicesTable(services);
                    setStatus("Загружено: " + services.size() + " услуг");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void loadCategories() {
        prepareSection("categories", "Загрузка категорий...");
        new Thread(() -> {
            try {
                // Категории тоже ограничиваем организацией, чтобы админ не видел чужие справочники.
                List<CategoryService> items = isOrganizationBound() && hasCurrentProvider()
                        ? ApiService.getCategoriesByProvider(currentProviderId())
                        : isOrganizationBound() ? List.of() : ApiService.getAllCategories();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(items);
                    setupCategoriesTable(items);
                    setStatus("Загружено: " + items.size() + " категорий");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void loadProviders() {
        prepareSection("providers", "Загрузка провайдеров...");
        new Thread(() -> {
            try {
                // Обычный admin получает только свою организацию, superAdmin получает все.
                List<Provider> items = isOrganizationBound()
                        ? currentProviderAsList()
                        : ApiService.getAllProviders();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(items);
                    setupProvidersTable(items);
                    setStatus("Загружено: " + items.size() + " провайдеров");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void loadSpecializations() {
        prepareSection("specializations", "Загрузка специализаций...");
        new Thread(() -> {
            try {
                // Специализации являются справочником организации, поэтому фильтруем по providerId.
                List<Specialization> items = isOrganizationBound() && hasCurrentProvider()
                        ? ApiService.getSpecializationsByProvider(currentProviderId())
                        : isOrganizationBound() ? List.of() : ApiService.getAllSpecializations();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(items);
                    setupSpecializationsTable(items);
                    setStatus("Загружено: " + items.size() + " специализаций");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void loadPayments() {
        prepareSection("payments", "Загрузка платежей...");
        new Thread(() -> {
            try {
                // Платежи админ организации смотрит только по своей организации.
                List<Payment> items = isOrganizationBound() && hasCurrentProvider()
                        ? ApiService.getPaymentsByProvider(currentProviderId())
                        : isOrganizationBound() ? List.of() : ApiService.getAllPayments();
                Platform.runLater(() -> {
                    currentData = new ArrayList<>(items);
                    setupPaymentsTable(items);
                    setStatus("Загружено: " + items.size() + " платежей");
                });
            } catch (Exception e) {
                reportLoadError(e);
            }
        }).start();
    }

    private void reportLoadError(Exception e) {
        AppLogger.warn("Ошибка загрузки раздела админки", e);
        Platform.runLater(() -> setStatus("Ошибка: " + readableError(e)));
    }

    // ==================== ТАБЛИЦЫ ====================

    @SuppressWarnings("unchecked")
    private void setupUsersTable(List<User> users) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colName = col("Имя", 170);
        TableColumn<Object, String> colEmail = col("Email", 220);
        TableColumn<Object, String> colProvider = col("Провайдер", 180);
        TableColumn<Object, String> colRoles = col("Роли", 180);
        TableColumn<Object, String> colSpecs = col("Специализации", 220);

        colName.setCellValueFactory(d -> prop(((User) d.getValue()).getName()));
        colEmail.setCellValueFactory(d -> prop(((User) d.getValue()).getEmail()));
        colProvider.setCellValueFactory(d -> prop(((User) d.getValue()).getProviderName()));
        colRoles.setCellValueFactory(d -> prop(String.join(", ", ((User) d.getValue()).getRoles())));
        colSpecs.setCellValueFactory(d -> prop(String.join(", ", ((User) d.getValue()).getSpecializations())));

        adminTable.getColumns().addAll(colName, colEmail, colProvider, colRoles, colSpecs);
        adminTable.setItems(FXCollections.observableArrayList(users));
    }

    @SuppressWarnings("unchecked")
    private void setupServicesTable(List<ProviderService> services) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colName = col("Название", 240);
        TableColumn<Object, String> colCat = col("Категория", 180);
        TableColumn<Object, String> colProv = col("Провайдер", 180);
        TableColumn<Object, String> colAccount = col("Счет", 180);
        TableColumn<Object, String> colSpecs = col("Специализации", 240);

        colName.setCellValueFactory(d -> prop(((ProviderService) d.getValue()).getName()));
        colCat.setCellValueFactory(d -> prop(((ProviderService) d.getValue()).getCategoryName()));
        colProv.setCellValueFactory(d -> prop(((ProviderService) d.getValue()).getProviderName()));
        colAccount.setCellValueFactory(d -> prop(((ProviderService) d.getValue()).getAccountName()));
        colSpecs.setCellValueFactory(d -> prop(String.join(", ", ((ProviderService) d.getValue()).getSpecializations())));

        adminTable.getColumns().addAll(colName, colCat, colProv, colAccount, colSpecs);
        adminTable.setItems(FXCollections.observableArrayList(services));
    }

    @SuppressWarnings("unchecked")
    private void setupCategoriesTable(List<CategoryService> items) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colName = col("Название", 240);
        TableColumn<Object, String> colParent = col("Родитель", 220);
        TableColumn<Object, String> colProv = col("Провайдер", 220);

        colName.setCellValueFactory(d -> prop(((CategoryService) d.getValue()).getName()));
        colParent.setCellValueFactory(d -> prop(((CategoryService) d.getValue()).getParentCategoryName()));
        colProv.setCellValueFactory(d -> prop(((CategoryService) d.getValue()).getProviderName()));

        adminTable.getColumns().addAll(colName, colParent, colProv);
        adminTable.setItems(FXCollections.observableArrayList(items));
    }

    @SuppressWarnings("unchecked")
    private void setupProvidersTable(List<Provider> items) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colFull = col("Полное название", 260);
        TableColumn<Object, String> colShort = col("Краткое", 180);
        TableColumn<Object, String> colLvl = col("Уровень комиссии", 180);
        TableColumn<Object, String> colCommission = col("Комиссия", 150);

        colFull.setCellValueFactory(d -> prop(((Provider) d.getValue()).getFullName()));
        colShort.setCellValueFactory(d -> prop(((Provider) d.getValue()).getShortName()));
        colLvl.setCellValueFactory(d -> prop(((Provider) d.getValue()).getCommLvl()));
        colCommission.setCellValueFactory(d -> prop(((Provider) d.getValue()).getCommission() == null
                ? "—"
                : ((Provider) d.getValue()).getCommission().getDisplayText()));

        adminTable.getColumns().addAll(colFull, colShort, colLvl, colCommission);
        adminTable.setItems(FXCollections.observableArrayList(items));
    }

    @SuppressWarnings("unchecked")
    private void setupSpecializationsTable(List<Specialization> items) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colName = col("Название", 260);
        TableColumn<Object, String> colProv = col("Провайдер", 240);

        colName.setCellValueFactory(d -> prop(((Specialization) d.getValue()).getName()));
        colProv.setCellValueFactory(d -> prop(((Specialization) d.getValue()).getProviderName()));

        adminTable.getColumns().addAll(colName, colProv);
        adminTable.setItems(FXCollections.observableArrayList(items));
    }

    @SuppressWarnings("unchecked")
    private void setupPaymentsTable(List<Payment> items) {
        adminTable.getColumns().clear();
        TableColumn<Object, String> colId = col("ID", 260);
        TableColumn<Object, String> colProvider = col("Провайдер", 170);
        TableColumn<Object, String> colSum = col("Сумма", 110);
        TableColumn<Object, String> colFee = col("Комиссия", 110);
        TableColumn<Object, String> colStatus = col("Статус", 120);
        TableColumn<Object, String> colCreated = col("Создан", 180);

        colId.setCellValueFactory(d -> prop(((Payment) d.getValue()).getId()));
        colProvider.setCellValueFactory(d -> prop(((Payment) d.getValue()).getProviderName()));
        colSum.setCellValueFactory(d -> prop(((Payment) d.getValue()).getSum().toString()));
        colFee.setCellValueFactory(d -> prop(((Payment) d.getValue()).getFee().toString()));
        colStatus.setCellValueFactory(d -> prop(((Payment) d.getValue()).getStatus()));
        colCreated.setCellValueFactory(d -> prop(((Payment) d.getValue()).getCreatedAt()));

        adminTable.getColumns().addAll(colId, colProvider, colSum, colFee, colStatus, colCreated);
        adminTable.setItems(FXCollections.observableArrayList(items));
    }

    // ==================== CRUD ====================

    @FXML
    private void onAdd() {
        if ("payments".equals(currentSection)) {
            showInfo("Платежи создаются из киоска", "В админке платежи можно смотреть, фильтровать и менять статус.");
            return;
        }
        showInputDialog("Добавить запись", null);
    }

    @FXML
    private void onEdit() {
        Object selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (selected instanceof Payment payment) {
            showPaymentStatusDialog(payment);
            return;
        }
        showInputDialog("Редактировать", selected);
    }

    @FXML
    private void onDelete() {
        Object selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (selected instanceof Provider && isOrganizationBound()) {
            showInfo("Удаление организации недоступно", "Админ организации может редактировать свою организацию, но не удалять ее.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление");
        confirm.setHeaderText("Удалить запись?");
        confirm.setContentText("Действие нельзя отменить. Если запись связана с другими данными, ApiAB может отклонить удаление.");
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
                else if (item instanceof Specialization s) success = ApiService.deleteSpecialization(s.getId());
                else if (item instanceof Payment p) success = ApiService.deletePayment(p.getId());

                boolean finalSuccess = success;
                Platform.runLater(() -> {
                    setStatus(finalSuccess ? "Запись удалена" : "ApiAB не подтвердил удаление");
                    refreshCurrentSection();
                });
            } catch (Exception e) {
                AppLogger.warn("Ошибка удаления", e);
                Platform.runLater(() -> setStatus("Ошибка удаления: " + readableError(e)));
            }
        }).start();
    }

    private void showInputDialog(String title, Object existingItem) {
        try {
            // Перед формой загружаем справочники, потому что ComboBox берет значения из них.
            refreshLookups();
        } catch (Exception e) {
            showError("Справочники не загружены", "Не удалось загрузить данные для формы: " + readableError(e));
            return;
        }

        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        buildForm(grid, existingItem, dialog);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(data -> {
            if (!data.isEmpty()) {
                performSave(existingItem, data);
            }
        });
    }

    private void buildForm(GridPane grid, Object item, Dialog<Map<String, Object>> dialog) {
        if ("services".equals(currentSection) || item instanceof ProviderService) {
            ProviderService service = item instanceof ProviderService s ? s : null;
            // Форма услуги собирается из справочников: категория, провайдер, счет, комиссия, специализации.
            TextField name = textField(service == null ? "" : service.getName());
            ComboBox<CategoryService> category = combo(categories, findByName(categories, CategoryService::getName, service == null ? null : service.getCategoryName()));
            ComboBox<Provider> provider = combo(providers, findProvider(service == null ? null : service.getProviderName()));
            lockProviderCombo(provider);
            ComboBox<Account> account = combo(accounts, findByName(accounts, Account::getName, service == null ? null : service.getAccountName()));
            ComboBox<Commission> commission = combo(commissions, findCommission(service == null ? null : service.getCommission()));
            commission.setPromptText("По умолчанию");
            ListView<Specialization> specs = listView(specializations);
            if (service != null) {
                selectByNames(specs, service.getSpecializations(), Specialization::getName);
            }

            addRow(grid, 0, "Название", name);
            addRow(grid, 1, "Категория", category);
            addRow(grid, 2, "Провайдер", provider);
            addRow(grid, 3, "Счет", account);
            addRow(grid, 4, "Комиссия", commission);
            addRow(grid, 5, "Специализации", specs);

            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                if (blank(name.getText()) || category.getValue() == null || provider.getValue() == null || account.getValue() == null) {
                    setStatus("Заполните название, категорию, провайдера и счет");
                    return Map.of();
                }
                Map<String, Object> data = new HashMap<>();
                // ApiAB ожидает id справочников, поэтому из ComboBox отправляем именно id.
                data.put("name", name.getText().trim());
                data.put("categoryId", category.getValue().getId());
                data.put("provId", provider.getValue().getId());
                data.put("accountId", account.getValue().getId());
                if (commission.getValue() != null) data.put("commId", commission.getValue().getId());
                data.put("specializationIds", specs.getSelectionModel().getSelectedItems().stream()
                        .map(Specialization::getId).collect(Collectors.toList()));
                return data;
            });
            return;
        }

        if ("categories".equals(currentSection) || item instanceof CategoryService) {
            CategoryService categoryItem = item instanceof CategoryService c ? c : null;
            // Категория привязывается к провайдеру, поэтому в форме всегда есть provider.
            TextField name = textField(categoryItem == null ? "" : categoryItem.getName());
            ComboBox<CategoryService> parent = combo(categories, findByName(categories, CategoryService::getName, categoryItem == null ? null : categoryItem.getParentCategoryName()));
            parent.setPromptText("Без родителя");
            ComboBox<Provider> provider = combo(providers, findProvider(categoryItem == null ? null : categoryItem.getProviderName()));
            lockProviderCombo(provider);
            addRow(grid, 0, "Название", name);
            addRow(grid, 1, "Родитель", parent);
            addRow(grid, 2, "Провайдер", provider);
            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                if (blank(name.getText()) || provider.getValue() == null) return Map.of();
                Map<String, Object> data = new HashMap<>();
                data.put("name", name.getText().trim());
                data.put("provId", provider.getValue().getId());
                if (parent.getValue() != null) data.put("prntCategory", parent.getValue().getId());
                return data;
            });
            return;
        }

        if ("providers".equals(currentSection) || item instanceof Provider) {
            Provider providerItem = item instanceof Provider p ? p : null;
            // Провайдер хранит уровень комиссии и саму комиссию, поэтому подтягиваем оба справочника.
            TextField fullName = textField(providerItem == null ? "" : providerItem.getFullName());
            TextField shortName = textField(providerItem == null ? "" : providerItem.getShortName());
            ComboBox<CommissionLevel> level = combo(commissionLevels, findByName(commissionLevels, CommissionLevel::getName, providerItem == null ? null : providerItem.getCommLvl()));
            ComboBox<Commission> commission = combo(commissions, findCommission(providerItem == null ? null : providerItem.getCommission()));
            commission.setPromptText("Без комиссии");
            addRow(grid, 0, "Полное название", fullName);
            addRow(grid, 1, "Краткое название", shortName);
            addRow(grid, 2, "Уровень комиссии", level);
            addRow(grid, 3, "Комиссия", commission);
            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                if (blank(fullName.getText()) || level.getValue() == null) return Map.of();
                Map<String, Object> data = new HashMap<>();
                data.put("fullName", fullName.getText().trim());
                data.put("shortName", shortName.getText().trim());
                data.put("commLvl", level.getValue().getId());
                if (commission.getValue() != null) data.put("commId", commission.getValue().getId());
                return data;
            });
            return;
        }

        if ("specializations".equals(currentSection) || item instanceof Specialization) {
            Specialization spec = item instanceof Specialization s ? s : null;
            // Специализация принадлежит провайдеру, обычному админу провайдер фиксируем.
            TextField name = textField(spec == null ? "" : spec.getName());
            ComboBox<Provider> provider = combo(providers, findProvider(spec == null ? null : spec.getProviderName()));
            lockProviderCombo(provider);
            addRow(grid, 0, "Название", name);
            addRow(grid, 1, "Провайдер", provider);
            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                if (blank(name.getText()) || provider.getValue() == null) return Map.of();
                return Map.of("name", name.getText().trim(), "provId", provider.getValue().getId());
            });
            return;
        }

        if ("users".equals(currentSection) || item instanceof User) {
            User user = item instanceof User u ? u : null;
            // Пользователь связан с провайдером, ролями и специализациями.
            TextField name = textField(user == null ? "" : user.getName());
            TextField email = textField(user == null ? "" : user.getEmail());
            TextField password = textField(user == null ? "" : user.getPassword());
            ComboBox<Provider> provider = combo(providers, findProvider(user == null ? null : user.getProviderName()));
            lockProviderCombo(provider);
            ListView<Role> roleList = listView(roles);
            ListView<Specialization> specList = listView(specializations);
            if (user != null) {
                selectByNames(roleList, user.getRoles(), Role::getName);
                selectByNames(specList, user.getSpecializations(), Specialization::getName);
            }

            addRow(grid, 0, "Имя", name);
            addRow(grid, 1, "Email", email);
            addRow(grid, 2, "Пароль", password);
            addRow(grid, 3, "Провайдер", provider);
            addRow(grid, 4, "Роли", roleList);
            addRow(grid, 5, "Специализации", specList);

            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                if (blank(name.getText()) || blank(email.getText()) || blank(password.getText()) || provider.getValue() == null) {
                    return Map.of();
                }
                Map<String, Object> data = new HashMap<>();
                data.put("name", name.getText().trim());
                data.put("email", email.getText().trim());
                data.put("password", password.getText().trim());
                data.put("provId", provider.getValue().getId());
                data.put("roleIds", roleList.getSelectionModel().getSelectedItems().stream().map(Role::getId).collect(Collectors.toList()));
                data.put("specializationIds", specList.getSelectionModel().getSelectedItems().stream().map(Specialization::getId).collect(Collectors.toList()));
                return data;
            });
        }
    }

    private void performSave(Object existingItem, Map<String, Object> data) {
        new Thread(() -> {
            try {
                // Если записи еще нет, вызываем create нужного раздела.
                if (existingItem == null) {
                    switch (currentSection) {
                        case "services" -> ApiService.createService(data);
                        case "categories" -> ApiService.createCategory(data);
                        case "providers" -> ApiService.createProvider(data);
                        case "specializations" -> ApiService.createSpecialization(data);
                        case "users" -> ApiService.createUser(data);
                        default -> throw new IllegalStateException("Выберите раздел для добавления");
                    }
                // Если запись есть, обновляем ее по id.
                } else if (existingItem instanceof ProviderService s) ApiService.updateService(s.getId(), data);
                else if (existingItem instanceof CategoryService c) ApiService.updateCategory(c.getId(), data);
                else if (existingItem instanceof Provider p) ApiService.updateProvider(p.getId(), data);
                else if (existingItem instanceof Specialization s) ApiService.updateSpecialization(s.getId(), data);
                else if (existingItem instanceof User u) ApiService.updateUser(u.getId(), data);

                Platform.runLater(() -> {
                    setStatus("Сохранено");
                    refreshCurrentSection();
                });
            } catch (Exception e) {
                AppLogger.warn("Ошибка сохранения", e);
                Platform.runLater(() -> setStatus("Ошибка сохранения: " + readableError(e)));
            }
        }).start();
    }

    private void showPaymentStatusDialog(Payment payment) {
        // В админке платеж не создается, здесь только меняется его статус.
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Изменить статус платежа");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("processing", "success", "cancel"));
        status.getSelectionModel().select(payment.getStatus());
        dialog.getDialogPane().setContent(status);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? status.getValue() : null);
        dialog.showAndWait().ifPresent(newStatus -> new Thread(() -> {
            try {
                ApiService.updatePaymentStatus(payment.getId(), newStatus);
                AppLogger.info("Статус платежа " + payment.getId() + " изменен на " + newStatus);
                Platform.runLater(this::loadPayments);
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Ошибка смены статуса: " + readableError(e)));
            }
        }).start());
    }

    private void refreshCurrentSection() {
        switch (currentSection) {
            case "users" -> loadUsers();
            case "services" -> loadServices();
            case "categories" -> loadCategories();
            case "providers" -> loadProviders();
            case "specializations" -> loadSpecializations();
            case "payments" -> loadPayments();
        }
    }

    // ==================== ФИЛЬТРЫ ====================

    @FXML
    private void onAdminSearch() {
        String q = adminSearchField.getText().trim().toLowerCase();
        List<Object> filtered = currentData.stream()
                .filter(item -> q.isEmpty() || getItemSearchText(item).toLowerCase().contains(q))
                .filter(this::matchesPaymentFilters)
                .collect(Collectors.toList());
        adminTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onResetPaymentFilters() {
        paymentStatusFilter.getSelectionModel().select("Все");
        minAmountField.clear();
        maxAmountField.clear();
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
        onAdminSearch();
    }

    private boolean matchesPaymentFilters(Object item) {
        if (!(item instanceof Payment payment) || !"payments".equals(currentSection)) {
            return true;
        }
        String status = paymentStatusFilter.getValue();
        if (status != null && !"Все".equals(status) && !status.equals(payment.getStatus())) {
            return false;
        }
        BigDecimal min = parseAmount(minAmountField.getText());
        BigDecimal max = parseAmount(maxAmountField.getText());
        if (min != null && payment.getSum().compareTo(min) < 0) {
            return false;
        }
        if (max != null && payment.getSum().compareTo(max) > 0) {
            return false;
        }
        LocalDate created = parseDate(payment.getCreatedAt());
        if (created != null && dateFromPicker.getValue() != null && created.isBefore(dateFromPicker.getValue())) {
            return false;
        }
        return created == null || dateToPicker.getValue() == null || !created.isAfter(dateToPicker.getValue());
    }

    private String getItemSearchText(Object item) {
        if (item instanceof User u) return u.getName() + " " + u.getEmail() + " " + u.getProviderName();
        if (item instanceof ProviderService s) return s.getName() + " " + s.getProviderName() + " " + s.getCategoryName();
        if (item instanceof CategoryService c) return c.getName() + " " + c.getProviderName();
        if (item instanceof Provider p) return p.getFullName() + " " + p.getShortName();
        if (item instanceof Specialization sp) return sp.getName() + " " + sp.getProviderName();
        if (item instanceof Payment p) return p.getId() + " " + p.getProviderName() + " " + p.getStatus();
        return "";
    }

    // ==================== HELPERS ====================

    private void refreshLookups() throws Exception {
        // Справочники для обычного админа заранее режем по его организации.
        providers = isOrganizationBound() ? currentProviderAsList() : ApiService.getAllProviders();
        categories = isOrganizationBound() && hasCurrentProvider() ? ApiService.getCategoriesByProvider(currentProviderId()) : isOrganizationBound() ? List.of() : ApiService.getAllCategories();
        accounts = isOrganizationBound() && hasCurrentProvider() ? ApiService.getAccountsByProvider(currentProviderId()) : isOrganizationBound() ? List.of() : ApiService.getAllAccounts();
        commissions = ApiService.getAllCommissions();
        commissionLevels = ApiService.getAllCommissionLevels();
        roles = ApiService.getAllRoles();
        specializations = isOrganizationBound() && hasCurrentProvider() ? ApiService.getSpecializationsByProvider(currentProviderId()) : isOrganizationBound() ? List.of() : ApiService.getAllSpecializations();
    }

    private boolean isOrganizationBound() {
        SessionManager session = SessionManager.getInstance();
        // superAdmin видит все, обычный admin ограничен своей организацией.
        return session.isAdmin() && !session.isSuperAdmin();
    }

    private boolean hasCurrentProvider() {
        return currentProviderId() != null && !currentProviderId().isBlank();
    }

    private String currentProviderId() {
        return SessionManager.getInstance().getCurrentProviderId();
    }

    private List<Provider> currentProviderAsList() throws Exception {
        String providerId = currentProviderId();
        if (providerId == null || providerId.isBlank()) {
            return List.of();
        }
        Provider provider = ApiService.getProviderById(providerId);
        return provider == null ? List.of() : List.of(provider);
    }

    private TextField textField(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPrefWidth(320);
        field.getStyleClass().add("dialog-field");
        return field;
    }

    private <T> ComboBox<T> combo(List<T> items, T selected) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(items));
        combo.setPrefWidth(320);
        if (selected != null) {
            combo.getSelectionModel().select(selected);
        }
        return combo;
    }

    private void lockProviderCombo(ComboBox<Provider> combo) {
        if (!isOrganizationBound()) {
            return;
        }
        // Обычный админ не выбирает провайдера вручную: он всегда работает только со своей организацией.
        if (!providers.isEmpty()) {
            combo.getSelectionModel().select(providers.get(0));
        }
        combo.setDisable(true);
    }

    private <T> ListView<T> listView(List<T> items) {
        ListView<T> view = new ListView<>(FXCollections.observableArrayList(items));
        view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        view.setPrefHeight(120);
        view.setPrefWidth(320);
        return view;
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node control) {
        grid.add(new Label(label + ":"), 0, row);
        grid.add(control, 1, row);
    }

    private <T> T findByName(List<T> items, Function<T, String> getter, String name) {
        if (name == null) {
            return null;
        }
        return items.stream()
                .filter(item -> name.equalsIgnoreCase(nullToEmpty(getter.apply(item))))
                .findFirst()
                .orElse(null);
    }

    private Provider findProvider(String providerName) {
        if (providerName == null) {
            return null;
        }
        return providers.stream().filter(p -> p.matchesName(providerName)).findFirst().orElse(null);
    }

    private Commission findCommission(Commission value) {
        if (value == null) {
            return null;
        }
        return commissions.stream()
                .filter(c -> Objects.equals(c.getCommissionType(), value.getCommissionType())
                        && Double.compare(c.getCommissionValue(), value.getCommissionValue()) == 0)
                .findFirst()
                .orElse(null);
    }

    private <T> void selectByNames(ListView<T> view, List<String> names, Function<T, String> getter) {
        if (names == null || names.isEmpty()) {
            return;
        }
        for (T item : view.getItems()) {
            if (names.stream().anyMatch(name -> name.equalsIgnoreCase(nullToEmpty(getter.apply(item))))) {
                view.getSelectionModel().select(item);
            }
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void setStatus(String text) {
        adminStatus.setText(text);
    }

    private <T> TableColumn<Object, T> col(String title, double width) {
        TableColumn<Object, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        return col;
    }

    private SimpleStringProperty prop(String value) {
        return new SimpleStringProperty(value == null || value.isBlank() ? "—" : value);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String readableError(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
