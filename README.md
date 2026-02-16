# 🎮 Minecraft-Ollama Bridge

MinecraftサーバーとOllama AI Botを接続するプラグイン＋APIブリッジシステム

## 📋 システム構成

```
┌─────────────────────┐
│ Minecraft Server    │
│ (Spigot/Paper)      │
│   + Plugin          │
└──────────┬──────────┘
           │ HTTP/WebSocket
           ↓
┌─────────────────────┐
│ API Bridge Server   │
│ (FastAPI/Python)    │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ Discord Bot         │
│ (Ollama Bot)        │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ Ollama LLM          │
│ (llama3/etc)        │
└─────────────────────┘
```

## ✨ 機能

### Minecraftプラグイン
- 🤖 `/ollama ask <質問>` - プレイヤーがAIに質問
- 🧠 `/ollama memory clear` - 会話履歴をクリア
- 📢 `/ollama broadcast <メッセージ>` - Discordに送信（要権限）
- 🔄 会話コンテキスト保持（プレイヤーごと）
- 📊 プレイヤー参加/退出の自動通知

### APIブリッジ
- RESTful API エンドポイント
- WebSocketによるリアルタイム通信
- プレイヤー情報トラッキング
- 会話メモリ管理
- Discord連携機能

## 🚀 セットアップ

### 1. 前提条件

- **Minecraft Server**: Spigot/Paper 1.19+
- **Python**: 3.11+
- **Ollama**: インストール済み
- **Discord Bot**: 既存のOllama Bot

### 2. APIサーバーのセットアップ

```bash
# リポジトリのクローン
cd minecraft-ollama-bridge

# Python仮想環境の作成
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 依存関係のインストール
pip install fastapi uvicorn websockets

# .envファイルの設定
cp .env.example .env
# DISCORD_TOKEN, OLLAMA_HOSTなどを設定

# APIサーバーの起動
python api_server.py
```

サーバーは `http://localhost:8000` で起動します。

### 3. Minecraftプラグインのビルド

```bash
# Mavenでビルド
mvn clean package

# 生成されたJARファイルを確認
ls target/OllamaBridge-1.0.0.jar
```

### 4. プラグインのインストール

```bash
# JARファイルをMinecraftサーバーのpluginsフォルダにコピー
cp target/OllamaBridge-1.0.0.jar /path/to/minecraft/plugins/

# サーバーを起動または再起動
# プラグインフォルダにconfig.ymlが生成される
```

### 5. 設定ファイルの編集

`plugins/OllamaBridge/config.yml` を編集：

```yaml
api:
  url: "http://localhost:8000"  # APIサーバーのURL
  use-websocket: false          # WebSocketを使う場合はtrue

broadcast:
  player-join: true   # プレイヤー参加をDiscordに通知
  player-quit: true   # プレイヤー退出をDiscordに通知

ai:
  use-memory: true    # 会話履歴を使用
  timeout: 180        # タイムアウト（秒）
```

## 💡 使い方

### プレイヤーコマンド

#### AIに質問
```
/ollama ask こんにちは！
/ollama ask Minecraftでダイヤモンドを見つける方法は？
/ollama ask Pythonとは何ですか？
```

#### 会話履歴のクリア
```
/ollama memory clear
```

### 管理者コマンド

#### Discordにブロードキャスト
```
/ollama broadcast サーバーメンテナンスを行います
```

### API直接アクセス

```bash
# ヘルスチェック
curl http://localhost:8000/

# チャット送信
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{
    "player": "TestPlayer",
    "message": "こんにちは",
    "use_memory": true
  }'

# メモリクリア
curl -X DELETE http://localhost:8000/memory/TestPlayer

# Discordにブロードキャスト
curl -X POST http://localhost:8000/broadcast \
  -H "Content-Type: application/json" \
  -d '{"message": "テストメッセージ"}'
```

## 🔧 APIエンドポイント

### REST API

| Method | Endpoint | 説明 |
|--------|----------|------|
| GET | `/` | ヘルスチェック |
| POST | `/chat` | チャット送信 |
| POST | `/broadcast` | Discordブロードキャスト |
| POST | `/player/update` | プレイヤー情報更新 |
| GET | `/player/{name}` | プレイヤー情報取得 |
| DELETE | `/memory/{name}` | メモリクリア |

### WebSocket

```
ws://localhost:8000/ws
```

メッセージ形式：
```json
{
  "type": "chat",
  "player": "PlayerName",
  "message": "質問内容"
}
```

## 🎨 カスタマイズ

### プラグイン側

`CommandHandler.java` でコマンドの動作を変更：
```java
// レスポンスの表示形式を変更
player.sendMessage(ChatColor.AQUA + "🤖 AI: " + response);
```

### API側

`api_server.py` でプロンプトをカスタマイズ：
```python
# システムプロンプトを追加
prompt = f"あなたはMinecraftの専門家です。\n質問: {question}"
```

## 🔐 権限

| 権限 | 説明 | デフォルト |
|------|------|-----------|
| `ollamabridge.use` | AI機能を使用 | true |
| `ollamabridge.broadcast` | Discordブロードキャスト | op |
| `ollamabridge.admin` | 管理者権限 | op |

## 📊 システム要件

- **RAM**: 最低4GB（Minecraft + API + Ollama）
- **CPU**: 4コア以上推奨
- **ディスク**: Ollamaモデル用に10GB+
- **ネットワーク**: ローカルネットワーク推奨

## 🐛 トラブルシューティング

### プラグインがAPIに接続できない

1. APIサーバーが起動しているか確認
   ```bash
   curl http://localhost:8000/
   ```

2. `config.yml`のURLが正しいか確認

3. ファイアウォールの確認

### AIの応答が遅い

1. Ollamaのモデルサイズを確認
    - 小型モデル推奨: `llama3:8b`, `mistral:7b`

2. タイムアウト設定を調整
   ```yaml
   ai:
     timeout: 300  # 5分に延長
   ```

### メモリ不足エラー

1. Minecraftサーバーのメモリを増やす
   ```bash
   java -Xmx4G -Xms2G -jar server.jar
   ```

2. Ollamaのモデルを小型化

## 🔄 アップデート

```bash
# プラグインの更新
mvn clean package
cp target/OllamaBridge-1.0.0.jar /path/to/minecraft/plugins/

# サーバー再起動
/reload confirm
```

## 📝 ライセンス

MIT License

## 🤝 コントリビューション

プルリクエスト歓迎！

## 📚 関連リンク

- [Ollama](https://ollama.ai/)
- [Spigot Plugin Development](https://www.spigotmc.org/wiki/spigot-plugin-development/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Discord.py](https://discordpy.readthedocs.io/)

## 💬 サポート

質問やバグ報告は [Issues](https://github.com/woxloi/minecraft-ollama-bridge/issues) へ