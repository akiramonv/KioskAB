import com.fasterxml.jackson.databind.JsonNode;
import com.kiosk.api.ApiClient;
import com.kiosk.model.CartItem;
import com.kiosk.model.Payment;
import com.kiosk.model.Provider;
import com.kiosk.model.ProviderService;
import com.kiosk.model.Role;
import com.kiosk.model.User;
import com.kiosk.service.ApiService;
import com.kiosk.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManualTestSuite {
    private static final List<String> results = new ArrayList<>();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        long started = System.nanoTime();
        long memBefore = usedMemory();

        run("CartItem rounds amount and calculates total", ManualTestSuite::cartItemTotal);
        run("CartItem normalizes non-positive quantity", ManualTestSuite::cartItemQuantityFloor);
        run("Payment treats null amounts as zero", ManualTestSuite::paymentNulls);
        run("Provider matches short, full and display names", ManualTestSuite::providerNameMatching);
        run("Role and SessionManager enforce admin flags", ManualTestSuite::sessionRoles);
        run("SessionManager logout clears user context", ManualTestSuite::sessionLogout);
        run("Jackson ignores unknown fields in User", ManualTestSuite::jacksonUnknownFields);
        run("ProviderService handles fixed and free price", ManualTestSuite::providerServicePrice);
        run("ApiClient and ApiService parse mock REST responses", ManualTestSuite::mockApiSuccess);
        run("ApiClient rejects failed HTTP responses", ManualTestSuite::mockApiFailure);

        long durationMs = (System.nanoTime() - started) / 1_000_000;
        long memAfter = usedMemory();
        System.out.println("{");
        System.out.println("  \"passed\": " + passed + ",");
        System.out.println("  \"failed\": " + failed + ",");
        System.out.println("  \"durationMs\": " + durationMs + ",");
        System.out.println("  \"usedMemoryBeforeBytes\": " + memBefore + ",");
        System.out.println("  \"usedMemoryAfterBytes\": " + memAfter + ",");
        System.out.println("  \"results\": [");
        for (int i = 0; i < results.size(); i++) {
            System.out.print("    \"" + escape(results.get(i)) + "\"");
            System.out.println(i + 1 == results.size() ? "" : ",");
        }
        System.out.println("  ]");
        System.out.println("}");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void cartItemTotal() {
        ProviderService service = new ProviderService();
        service.setName("Internet");
        CartItem item = new CartItem(service, new BigDecimal("10.235"), 3);
        assertEquals(new BigDecimal("10.24"), item.getAmount(), "amount");
        assertEquals(new BigDecimal("30.72"), item.getTotal(), "total");
    }

    private static void cartItemQuantityFloor() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("5"), 0);
        assertEquals(1, item.getQuantity(), "quantity");
        assertEquals(new BigDecimal("5.00"), item.getTotal(), "total");
    }

    private static void paymentNulls() {
        Payment payment = new Payment();
        payment.setSum(null);
        payment.setFee(null);
        assertEquals(BigDecimal.ZERO, payment.getSum(), "sum");
        assertEquals(BigDecimal.ZERO, payment.getFee(), "fee");
        assertEquals(BigDecimal.ZERO, payment.getTotal(), "total");
    }

    private static void providerNameMatching() {
        Provider provider = new Provider();
        provider.setFullName("ОсОО Test Provider");
        provider.setShortName("Test");
        assertTrue(provider.matchesName("test"), "short name");
        assertTrue(provider.matchesName("осоо test provider"), "full name");
        assertTrue(provider.matchesName(provider.toString()), "display name");
        assertTrue(!provider.matchesName(null), "null must not match");
    }

    private static void sessionRoles() {
        SessionManager session = SessionManager.getInstance();
        session.logout();
        Role user = role("user");
        Role admin = role("admin");
        Role superAdmin = role("superAdmin");
        session.setCurrentUserRoles(Arrays.asList(user));
        assertTrue(!session.isAdmin(), "user role is not admin");
        session.setCurrentUserRoles(Arrays.asList(admin));
        assertTrue(session.isAdmin(), "admin role");
        assertTrue(!session.isSuperAdmin(), "admin is not superAdmin");
        session.setCurrentUserRoles(Arrays.asList(superAdmin));
        assertTrue(session.isAdmin(), "superAdmin is admin");
        assertTrue(session.isSuperAdmin(), "superAdmin flag");
    }

    private static void sessionLogout() {
        SessionManager session = SessionManager.getInstance();
        User user = new User();
        user.setId("u1");
        session.setCurrentUser(user);
        session.setCurrentProviderId("p1");
        session.setCurrentUserRoles(Arrays.asList(role("admin")));
        session.logout();
        assertTrue(!session.isLoggedIn(), "logged out");
        assertTrue(!session.hasProvider(), "provider cleared");
        assertTrue(!session.isAdmin(), "roles cleared");
    }

    private static void jacksonUnknownFields() throws Exception {
        String json = "{\"id\":\"u1\",\"name\":\"Akmaral\",\"email\":\"a@test.local\",\"unknown\":123,"
                + "\"roles\":[\"admin\"],\"specializations\":[\"cash\"]}";
        User user = ApiClient.getMapper().readValue(json, User.class);
        assertEquals("u1", user.getId(), "id");
        assertEquals("Akmaral", user.getName(), "name");
        assertEquals(1, user.getRoles().size(), "roles");
        assertEquals("cash", user.getSpecializations().get(0), "specialization");
    }

    private static void providerServicePrice() {
        ProviderService fixed = new ProviderService();
        fixed.setPrice(new BigDecimal("120"));
        assertTrue(fixed.hasFixedPrice(), "fixed price");
        assertEquals("120.00 сом", fixed.getDisplayPrice(), "display price");
        ProviderService free = new ProviderService();
        free.setPrice(null);
        assertTrue(!free.hasFixedPrice(), "free amount service");
        assertTrue(free.getDisplayPrice() == null, "free display price");
    }

    private static void mockApiSuccess() throws Exception {
        HttpServer server = startMockServer();
        try {
            List<ProviderService> services = ApiService.getAllServices();
            assertEquals(1, services.size(), "service count");
            assertEquals("Mock service", services.get(0).getName(), "service name");
            JsonNode execute = ApiService.executeOperation("ALL_SERVICES", null);
            assertTrue(execute.path("success").asBoolean(false), "execute success");
        } finally {
            server.stop(0);
        }
    }

    private static void mockApiFailure() throws Exception {
        HttpServer server = startMockServer();
        try {
            boolean thrown = false;
            try {
                ApiClient.get("/api/fail");
            } catch (IllegalStateException expected) {
                thrown = expected.getMessage().contains("mock failure");
            }
            assertTrue(thrown, "failure response throws");
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startMockServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 7924), 0);
        server.createContext("/api/services", exchange -> respond(exchange, 200,
                "{\"success\":true,\"data\":[{\"id\":\"s1\",\"name\":\"Mock service\",\"price\":15.5,"
                        + "\"categoryName\":\"Payments\",\"providerName\":\"Mock provider\"}]}"));
        server.createContext("/api/emulator/execute", exchange -> respond(exchange, 200,
                "{\"success\":true,\"data\":{\"operation\":\"ok\"}}"));
        server.createContext("/api/fail", exchange -> respond(exchange, 500,
                "{\"success\":false,\"message\":\"mock failure\"}"));
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static void run(String name, CheckedRunnable runnable) {
        try {
            runnable.run();
            passed++;
            results.add("PASS: " + name);
        } catch (Throwable t) {
            failed++;
            results.add("FAIL: " + name + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
