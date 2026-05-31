# Киоск самообслуживания — Java Desktop App

Полноценное JavaFX desktop-приложение в стиле терминала самообслуживания (KFC-кiosk).

## Требования

- **Java 17+** (Java 21 рекомендуется)
- **Maven 3.8+**
- **IntelliJ IDEA** 2023+ (Community или Ultimate)
- API Emulator запущен на `http://localhost:7924`

## Структура проекта

```
kiosk-app/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/kiosk/
    │       ├── KioskApp.java              ← точка входа
    │       ├── api/
    │       │   └── ApiClient.java         ← HTTP-клиент
    │       ├── model/                     ← модели данных
    │       │   ├── User.java
    │       │   ├── Role.java
    │       │   ├── ProviderService.java
    │       │   ├── CategoryService.java
    │       │   ├── Provider.java
    │       │   ├── Commission.java
    │       │   ├── Specialization.java
    │       │   └── Account.java
    │       ├── service/
    │       │   └── ApiService.java        ← все вызовы API
    │       ├── ui/
    │       │   ├── controllers/
    │       │   │   ├── MainController.java    ← главный экран
    │       │   │   ├── LoginController.java   ← авторизация
    │       │   │   ├── AdminController.java   ← панель админа
    │       │   │   └── ServiceDetailController.java
    │       │   └── components/
    │       │       ├── ServiceCard.java    ← карточка услуги
    │       │       └── CategoryButton.java ← кнопка категории
    │       └── util/
    │           └── SessionManager.java    ← синглтон сессии
    └── resources/
        ├── fxml/
        │   ├── main.fxml
        │   ├── login.fxml
        │   ├── service_detail.fxml
        │   └── admin.fxml
        └── css/
            └── styles.css                 ← бело-зелёная тема
```

## Запуск в IntelliJ IDEA

### Шаг 1 — Открыть проект
1. `File → Open` → выбери папку `kiosk-app`
2. IntelliJ автоматически распознает Maven-проект
3. Подожди, пока загрузятся зависимости (`maven sync`)

### Шаг 2 — Настроить Run Configuration
1. `Run → Edit Configurations → + → Application`
2. **Main class:** `com.kiosk.KioskApp`
3. **VM options:** (если нужно вручную, но обычно Maven-плагин справляется)
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```
4. **Или через Maven:** в терминале:
   ```bash
   mvn javafx:run
   ```

### Шаг 3 — Запустить API Emulator
Убедись, что API Emulator работает на `http://localhost:7924` перед запуском приложения.

## Возможности

| Функция | Реализовано |
|---------|------------|
| Просмотр всех услуг | ✅ |
| Фильтрация по категории | ✅ |
| Фильтрация по провайдеру | ✅ |
| Поиск по названию | ✅ |
| Авторизация пользователя | ✅ |
| Услуги по специализации | ✅ |
| Детали услуги (диалог) | ✅ |
| Избранные услуги | ✅ |
| Популярные услуги | ✅ |
| Панель администратора | ✅ |
| CRUD: пользователи | ✅ |
| CRUD: услуги | ✅ |
| CRUD: категории | ✅ |
| CRUD: провайдеры | ✅ |
| CRUD: специализации | ✅ |
| Бело-зелёная тема | ✅ |

## API Emulator Endpoints

Приложение использует `http://localhost:7924` через `ApiClient.java`.

Поддерживаемые операции через `/api/emulator/execute`:
- `ALL_SERVICES`, `SERVICE_BY_ID`, `SERVICES_BY_CATEGORY`
- `SERVICES_BY_PROVIDER`, `SERVICES_BY_USER_SPECIALIZATION`
- `ALL_CATEGORIES`, `CATEGORY_BY_ID`
- `ALL_PROVIDERS`, `PROVIDER_BY_ID`
- `ALL_SPECIALIZATIONS`, `SPECIALIZATION_BY_ID`
- `ALL_USERS`, `USER_BY_ID`, `USERS_BY_PROVIDER`, `USERS_BY_SPECIALIZATION`

## Архитектурные решения

- **MVC** — controllers ↔ service ↔ api
- **Singleton SessionManager** — хранит текущего пользователя, роли, специализации
- **Фоновые потоки** — все запросы к API выполняются в `new Thread()`, UI обновляется через `Platform.runLater()`
- **Jackson** — десериализация JSON-ответов в типизированные модели
- **Java HttpClient** — встроенный HTTP-клиент Java 11+ (без доп. зависимостей)
