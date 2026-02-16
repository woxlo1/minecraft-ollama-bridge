package com.woxloi.ollamabridge.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.woxloi.ollamabridge.OllamaBridgePlugin;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

/**
 * WebSocket client for real-time communication
 */
public class WebSocketClient implements WebSocket.Listener {

    private final String wsUrl;
    private final OllamaBridgePlugin plugin;
    private final Gson gson;
    private WebSocket webSocket;

    public WebSocketClient(String wsUrl, OllamaBridgePlugin plugin) {
        this.wsUrl = wsUrl;
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            CompletionStage<WebSocket> ws = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), this);

            this.webSocket = ws.toCompletableFuture().get();
            plugin.getLogger().info("WebSocket connected to: " + wsUrl);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect WebSocket", e);
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin disabled");
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return webSocket != null && !webSocket.isInputClosed();
    }

    /**
     * Send player event (join/leave)
     */
    public void sendPlayerEvent(String eventType, String playerName) {
        if (!isConnected()) return;

        JsonObject message = new JsonObject();
        message.addProperty("type", "player_" + eventType);
        message.addProperty("player", playerName);

        webSocket.sendText(gson.toJson(message), true);
    }

    /**
     * Send chat message
     */
    public void sendChat(String playerName, String message) {
        if (!isConnected()) return;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "chat");
        msg.addProperty("player", playerName);
        msg.addProperty("message", message);

        webSocket.sendText(gson.toJson(msg), true);
    }

    // WebSocket.Listener implementation

    @Override
    public void onOpen(WebSocket webSocket) {
        plugin.getLogger().info("WebSocket opened");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        // Handle incoming messages from server
        try {
            JsonObject message = gson.fromJson(data.toString(), JsonObject.class);
            String type = message.get("type").getAsString();

            // Handle different message types
            if ("chat_response".equals(type)) {
                String player = message.get("player").getAsString();
                String response = message.get("response").getAsString();

                // Send response to player on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var p = Bukkit.getPlayer(player);
                    if (p != null && p.isOnline()) {
                        p.sendMessage("🤖 AI: " + response);
                    }
                });
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error processing WebSocket message", e);
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "WebSocket error", error);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        plugin.getLogger().info("WebSocket closed: " + reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}