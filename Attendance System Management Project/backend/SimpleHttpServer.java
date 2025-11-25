import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

public class SimpleHttpServer {

    private static AttendanceManager manager;

    public static void main(String[] args) throws IOException {
        manager = new AttendanceManager();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/login/lecturer", new LecturerLoginHandler());
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/students/by-roll", new StudentByRollHandler());
        server.createContext("/api/students/attendance", new AttendanceHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("HTTP server started on http://localhost:8080");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] bytes = responseBody.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                map.put(key, value);
            } catch (Exception e) {
                map.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    private static String studentToJson(Student s) {
        if (s == null) return "null";
        double percentage = s.getAttendancePercentage();
        return String.format(
                Locale.US,
                "{\"id\":%d,\"name\":\"%s\",\"rollNo\":%d,\"totalClasses\":%d,\"attendedClasses\":%d,\"percentage\":%.2f}",
                s.getId(),
                escapeJson(s.getName()),
                s.getRollNo(),
                s.getTotalClasses(),
                s.getAttendedClasses(),
                percentage
        );
    }

    private static String studentsToJson(List<Student> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Student s : list) {
            if (!first) sb.append(",");
            sb.append(studentToJson(s));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    // --------- Handlers ---------

    // POST /api/login/lecturer?username=...&password=...
    static class LecturerLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String username = params.get("username");
            String password = params.get("password");

            if (username == null || password == null) {
                sendResponse(exchange, 400, "{\"success\":false,\"message\":\"Missing username or password\"}");
                return;
            }

            boolean ok = manager.validateLecturerLogin(username, password);
            if (ok) {
                sendResponse(exchange, 200, "{\"success\":true}");
            } else {
                sendResponse(exchange, 401, "{\"success\":false,\"message\":\"Invalid credentials\"}");
            }
        }
    }

    // GET/POST/DELETE /api/students
    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(method)) {
                handleOptions(exchange);
                return;
            }
            switch (method) {
                case "GET":
                    handleGetAll(exchange);
                    break;
                case "POST":
                    handleAdd(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }

        private void handleGetAll(HttpExchange exchange) throws IOException {
        List<Student> list = manager.getStudents();
        System.out.println("/api/students -> rows from DB: " + list.size());
        String json = studentsToJson(list);
        sendResponse(exchange, 200, json);
        }



        private void handleAdd(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String name = params.get("name");
            String rollStr = params.get("rollNo");
            if (name == null || rollStr == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing name or rollNo\"}");
                return;
            }
            try {
                int roll = Integer.parseInt(rollStr);
                manager.addStudent(name, roll);
                sendResponse(exchange, 200, "{\"success\":true}");
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid rollNo\"}");
            }
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String rollStr = params.get("rollNo");
            if (rollStr == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing rollNo\"}");
                return;
            }
            try {
                int roll = Integer.parseInt(rollStr);
                manager.deleteStudent(roll);
                sendResponse(exchange, 200, "{\"success\":true}");
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid rollNo\"}");
            }
        }
    }

    // GET /api/students/by-roll?rollNo=...
    static class StudentByRollHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(method)) {
                handleOptions(exchange);
                return;
            }
            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String rollStr = params.get("rollNo");
            if (rollStr == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing rollNo\"}");
                return;
            }
            try {
                int roll = Integer.parseInt(rollStr);
                Student s = manager.getStudentByRollNo(roll);
                if (s == null) {
                    sendResponse(exchange, 404, "{\"error\":\"Student not found\"}");
                } else {
                    sendResponse(exchange, 200, studentToJson(s));
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid rollNo\"}");
            }
        }
    }

    // POST /api/students/attendance?rollNo=&totalClasses=&attendedClasses=
    static class AttendanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(method)) {
                handleOptions(exchange);
                return;
            }
            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String rollStr      = params.get("rollNo");
            String totalStr     = params.get("totalClasses");
            String attendedStr  = params.get("attendedClasses");

            if (rollStr == null || totalStr == null || attendedStr == null) {
                sendResponse(exchange, 400, "{\"error\":\"Missing parameters\"}");
                return;
            }

            try {
                int roll     = Integer.parseInt(rollStr);
                int total    = Integer.parseInt(totalStr);
                int attended = Integer.parseInt(attendedStr);

                boolean ok = manager.markAttendance(roll, total, attended);
                if (!ok) {
                    sendResponse(exchange, 404,
                            "{\"success\":false,\"message\":\"Student not found for given roll number\"}");
                } else {
                    sendResponse(exchange, 200, "{\"success\":true}");
                }
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid numeric value\"}");
            }
        }
    }
}
