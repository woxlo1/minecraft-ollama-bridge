package com.woxloi.ollamabridge.command;

import com.woxloi.ollamabridge.OllamaBridgePlugin;
import com.woxloi.ollamabridge.api.ApiClient;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final OllamaBridgePlugin plugin;
    private final ApiClient apiClient;

    public CommandHandler(OllamaBridgePlugin plugin, ApiClient apiClient) {
        this.plugin = plugin;
        this.apiClient = apiClient;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("ollama")) {
            return false;
        }

        // 引数なし
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // =========================
        // /ollama ask <message>
        // =========================
        if (args[0].equalsIgnoreCase("ask")) {
            if (!sender.hasPermission("ollamabridge.use")) {
                sender.sendMessage(ChatColor.RED + "このコマンドを使用する権限がありません。");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "使用法: /ollama ask <質問>");
                return true;
            }

            Player player = (Player) sender;
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            player.sendMessage(ChatColor.GRAY + "🤖 AIに質問中... (質問内容: " + message + ")");

            plugin.sendAiChat(player, message, new OllamaBridgePlugin.ChatCallback() {

                @Override
                public void onResponse(String response) {
                    List<String> lines = splitResponse(response, 60);

                    player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage(ChatColor.AQUA + "🤖 AI回答:");
                    for (String line : lines) {
                        player.sendMessage(ChatColor.WHITE + line);
                    }
                    player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━");
                }

                @Override
                public void onError(String error) {
                    player.sendMessage(ChatColor.RED + "❌ " + error);
                }
            });

            return true;
        }

        // =========================
        // /ollama memory clear
        // =========================
        if (args[0].equalsIgnoreCase("memory")) {
            if (!sender.hasPermission("ollamabridge.use")) {
                sender.sendMessage(ChatColor.RED + "このコマンドを使用する権限がありません。");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
                return true;
            }

            if (args.length < 2 || !args[1].equalsIgnoreCase("clear")) {
                sender.sendMessage(ChatColor.YELLOW + "使用法: /ollama memory clear");
                return true;
            }

            Player player = (Player) sender;
            player.sendMessage(ChatColor.GRAY + "会話履歴をクリア中...");

            plugin.clearMemory(player.getName(), success -> {
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "✓ 会話履歴をクリアしました。");
                } else {
                    player.sendMessage(ChatColor.RED + "❌ 会話履歴のクリアに失敗しました。");
                }
            });

            return true;
        }

        // =========================
        // /ollama broadcast <message>
        // =========================
        if (args[0].equalsIgnoreCase("broadcast")) {

            if (!sender.hasPermission("ollamabridge.broadcast")) {
                sender.sendMessage(ChatColor.RED + "権限がありません。");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "使用法: /ollama broadcast <メッセージ>");
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            String fullMessage = sender instanceof Player
                    ? "[" + sender.getName() + "] " + message
                    : "[Server] " + message;

            plugin.broadcastToDiscord(fullMessage);
            sender.sendMessage(ChatColor.GREEN + "✓ Discordに送信しました: " + message);

            return true;
        }

        sender.sendMessage(ChatColor.RED + "不明なサブコマンドです。");
        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.AQUA + "Ollama Bridge コマンド一覧");
        sender.sendMessage(ChatColor.YELLOW + "/ollama ask <質問>");
        sender.sendMessage(ChatColor.YELLOW + "/ollama memory clear");
        sender.sendMessage(ChatColor.YELLOW + "/ollama broadcast <メッセージ>");
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {

        if (!command.getName().equalsIgnoreCase("ollama")) {
            return null;
        }

        if (args.length == 1) {
            return Arrays.asList("ask", "memory", "broadcast");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("memory")) {
            return Arrays.asList("clear");
        }

        return null;
    }

    private List<String> splitResponse(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
