package com.woxloi.ollamabridge.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * REST API client for Ollama Bridge
 */
public class ApiClient {

    private final String baseUrl;
    private final Gson gson;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.gson = new Gson();
    }

    /**
     * Test connection to API
     */
    public boolean testConnection() throws Exception {
        URL url = new URL(baseUrl + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        return responseCode == 200;
    }

    /**
     * Send chat message
     */
    public String sendChat(String playerName, String message) throws Exception {
        return sendChat(playerName, message, true);
    }

    /**
     * Send chat message with memory option
     */
    public String sendChat(String playerName, String message, boolean useMemory) throws Exception {
        URL url = new URL(baseUrl + "/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(180000); // 3 minutes for AI response

        // Build request
        JsonObject request = new JsonObject();
        request.addProperty("player", playerName);
        request.addProperty("message", message);
        request.addProperty("use_memory", useMemory);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = gson.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }

                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                return jsonResponse.get("response").getAsString();
            }
        } else {
            throw new Exception("API returned error code: " + responseCode);
        }
    }

    /**
     * Clear player's conversation memory
     */
    public boolean clearMemory(String playerName) throws Exception {
        URL url = new URL(baseUrl + "/memory/" + playerName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        conn.disconnect();

        return responseCode == 200;
    }

    /**
     * Broadcast message to Discord
     */
    public void broadcastToDiscord(String message) throws Exception {
        broadcastToDiscord(message, null);
    }

    /**
     * Broadcast message to specific Discord channel
     */
    public void broadcastToDiscord(String message, Integer channelId) throws Exception {
        URL url = new URL(baseUrl + "/broadcast");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        // Build request
        JsonObject request = new JsonObject();
        request.addProperty("message", message);
        if (channelId != null) {
            request.addProperty("channel_id", channelId);
        }

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = gson.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        conn.getResponseCode(); // Trigger request
        conn.disconnect();
    }

    /**
     * Update player information
     */
    public void updatePlayerInfo(String playerName, String uuid,
                                 double x, double y, double z,
                                 double health, String gamemode) throws Exception {
        URL url = new URL(baseUrl + "/player/update");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        // Build request
        JsonObject request = new JsonObject();
        request.addProperty("name", playerName);
        request.addProperty("uuid", uuid);

        JsonObject location = new JsonObject();
        location.addProperty("x", x);
        location.addProperty("y", y);
        location.addProperty("z", z);
        request.add("location", location);

        request.addProperty("health", health);
        request.addProperty("gamemode", gamemode);

        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = gson.toJson(request).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        conn.getResponseCode();
        conn.disconnect();
    }
}