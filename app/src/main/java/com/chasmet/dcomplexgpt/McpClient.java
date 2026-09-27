package com.chasmet.dcomplexgpt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class McpClient {
    public interface Callback {
        void onSuccess(String status);
        void onError(String error);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void testConnection(String endpoint, String token, Callback callback) {
        executor.execute(() -> {
            try {
                String url = normalize(endpoint);
                Response init = post(url, token, null,
                        jsonRpc(1, "initialize",
                                new JSONObject()
                                        .put("protocolVersion", "2025-06-18")
                                        .put("capabilities", new JSONObject())
                                        .put("clientInfo", new JSONObject()
                                                .put("name", "D-Complex GPT")
                                                .put("version", "1.0"))));

                if (init.code < 200 || init.code >= 300) {
                    callback.onError("MCP initialize HTTP " + init.code + " — " + compact(init.body));
                    return;
                }

                JSONObject initJson = parseJsonOrSse(init.body);
                if (initJson.has("error")) {
                    callback.onError("MCP initialize — " + compact(initJson.opt("error").toString()));
                    return;
                }

                Response initialized = post(url, token, init.sessionId,
                        new JSONObject()
                                .put("jsonrpc", "2.0")
                                .put("method", "notifications/initialized"));
                if (initialized.code < 200 || initialized.code >= 300) {
                    callback.onError("MCP initialized HTTP " + initialized.code);
                    return;
                }

                Response tools = post(url, token, init.sessionId,
                        jsonRpc(2, "tools/list", new JSONObject()));

                if (tools.code < 200 || tools.code >= 300) {
                    callback.onError("MCP tools/list HTTP " + tools.code + " — " + compact(tools.body));
                    return;
                }

                JSONObject toolsJson = parseJsonOrSse(tools.body);
                JSONObject result = toolsJson.optJSONObject("result");
                JSONArray list = result == null ? null : result.optJSONArray("tools");
                int count = list == null ? 0 : list.length();

                StringBuilder names = new StringBuilder();
                if (list != null) {
                    for (int i = 0; i < list.length() && i < 8; i++) {
                        JSONObject tool = list.optJSONObject(i);
                        if (tool == null) continue;
                        if (names.length() > 0) names.append(", ");
                        names.append(tool.optString("name", "outil"));
                    }
                }

                String status = "MCP connecté • " + count + " outil" + (count > 1 ? "s" : "");
                if (names.length() > 0) {
                    status += "\n" + names;
                }
                callback.onSuccess(status);

            } catch (Exception e) {
                callback.onError(e.getClass().getSimpleName() + " — " +
                        (e.getMessage() == null ? "Erreur MCP" : e.getMessage()));
            }
        });
    }

    private static JSONObject jsonRpc(int id, String method, JSONObject params) throws Exception {
        return new JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", id)
                .put("method", method)
                .put("params", params);
    }

    private static Response post(String endpoint, String token, String sessionId, JSONObject body)
            throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("MCP-Protocol-Version", "2025-06-18");
        connection.setRequestProperty("User-Agent", "D-Complex-GPT-Android");

        if (sessionId != null && !sessionId.isEmpty()) {
            connection.setRequestProperty("Mcp-Session-Id", sessionId);
        }
        if (token != null && !token.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
        }

        if (body != null) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        String responseBody = read(stream);
        String nextSessionId = connection.getHeaderField("Mcp-Session-Id");
        return new Response(code, responseBody, nextSessionId);
    }

    private static JSONObject parseJsonOrSse(String body) throws Exception {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("{")) {
            return new JSONObject(trimmed);
        }

        String[] lines = trimmed.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (data.startsWith("{")) {
                    return new JSONObject(data);
                }
            }
        }
        throw new IllegalStateException("Réponse MCP non reconnue");
    }

    private static String normalize(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim();
        if (!value.startsWith("https://")) {
            throw new IllegalArgumentException("L’URL MCP doit commencer par https://");
        }
        return value;
    }

    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static String compact(String value) {
        if (value == null) return "";
        String oneLine = value.replace('\n', ' ').trim();
        return oneLine.length() > 220 ? oneLine.substring(0, 220) + "…" : oneLine;
    }

    private static final class Response {
        final int code;
        final String body;
        final String sessionId;

        Response(int code, String body, String sessionId) {
            this.code = code;
            this.body = body;
            this.sessionId = sessionId;
        }
    }
}
