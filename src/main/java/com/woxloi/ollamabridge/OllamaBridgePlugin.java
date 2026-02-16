package com.woxloi.ollamabridge;

import com.woxloi.ollamabridge.api.ApiClient;
import com.woxloi.ollamabridge.api.WebSocketClient;
import com.woxloi.ollamabridge.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Ollama Bridge Plugin for Minecraft
 *
 * Connects Minecraft server to Ollama Bot via REST API
 */
public class OllamaBridgePlugin extends JavaPlugin {

    private ApiClient apiClient;
    private WebSocketClient wsClient;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize API client
        String apiUrl = getConfig().getString("api.url", "http://localhost:8000");
        apiClient = new ApiClient(apiUrl);

        // Initialize WebSocket (optional, for real-time)
        if (getConfig().getBoolean("api.use-websocket", false)) {
            String wsUrl = getConfig().getString("api.websocket-url", "ws://localhost:8000/ws");
            wsClient = new WebSocketClient(wsUrl, this);
            wsClient.connect();
        }

        // Register commands
        commandHandler = new CommandHandler(this, apiClient);
        getCommand("ollama").setExecutor(commandHandler);

        getLogger().info("Ollama Bridge Plugin enabled!");
        getLogger().info("API URL: " + apiUrl);

        // Test connection
        testConnection();
    }

    @Override
    public void onDisable() {
        if (wsClient != null && wsClient.isConnected()) {
            wsClient.disconnect();
        }

        getLogger().info("Ollama Bridge Plugin disabled!");
    }

    private void testConnection() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                boolean connected = apiClient.testConnection();
                if (connected) {
                    getLogger().info(ChatColor.GREEN + "Successfully connected to Ollama API!");
                } else {
                    getLogger().warning(ChatColor.RED + "Failed to connect to Ollama API!");
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error testing connection", e);
            }
        });
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public WebSocketClient getWsClient() {
        return wsClient;
    }

    /**
     * Send AI chat request
     */
    public void sendAiChat(Player player, String message, ChatCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String response = apiClient.sendChat(player.getName(), message);

                // Run callback on main thread
                Bukkit.getScheduler().runTask(this, () -> callback.onResponse(response));

            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error in AI chat", e);
                Bukkit.getScheduler().runTask(this, () ->
                        callback.onError("AI通信エラー: " + e.getMessage())
                );
            }
        });
    }

    /**
     * Clear player's conversation memory
     */
    public void clearMemory(String playerName, ClearMemoryCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                boolean success = apiClient.clearMemory(playerName);

                Bukkit.getScheduler().runTask(this, () -> callback.onComplete(success));

            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error clearing memory", e);
                Bukkit.getScheduler().runTask(this, () -> callback.onComplete(false));
            }
        });
    }

    /**
     * Broadcast message to Discord
     */
    public void broadcastToDiscord(String message) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                apiClient.broadcastToDiscord(message);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error broadcasting to Discord", e);
            }
        });
    }

    // Callback interfaces
    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public interface ClearMemoryCallback {
        void onComplete(boolean success);
    }
}