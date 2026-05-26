# Whatsup — Secure Real-Time Messaging App
**Team Coffcodes | B.Tech CSE Phase 3**

---

## Project Structure

```
whatsup/
├── src/
│   ├── App.java                  ← JavaFX entry point (client)
│   ├── model/
│   │   ├── User.java
│   │   ├── Message.java          ← Phase 3: added mediaPath, mediaType
│   │   └── Group.java
│   ├── database/
│   │   ├── DBConnection.java     ← SQLite (local) or PostgreSQL (cloud)
│   │   ├── UserDAO.java
│   │   ├── MessageDAO.java       ← Phase 3: media support
│   │   └── GroupDAO.java
│   ├── server/
│   │   ├── ChatServer.java       ← Run this on server machine (Akshat's work)
│   │   ├── ClientHandler.java    ← One thread per client (Akshat's work)
│   │   └── ChatClient.java       ← Client-side socket wrapper
│   └── ui/
│       ├── LoginScreen.java
│       ├── RegisterScreen.java
│       ├── HomeScreen.java
│       ├── ChatScreen.java       ← Phase 3: image/video sharing, read receipts
│       ├── GroupChatScreen.java  ← Phase 3: media in groups
│       └── NewChatScreen.java
├── pom.xml
└── README.md
```

---

## Phase 3 Features Added

- ✅ Image sharing (JPG, PNG, GIF, WebP) in private and group chats
- ✅ Video sharing (MP4, MOV) in private and group chats
- ✅ Read receipts (✓✓ tick marks on sent messages)
- ✅ Message timestamps on all bubbles
- ✅ Real-time server integration in ChatScreen and GroupChatScreen
- ✅ PostgreSQL support for persistent cloud database
- ✅ Connection status indicator (online/offline/reconnecting)
- ✅ Pending message delivery on reconnect (server side)
- 🔜 Profile photo upload (UI placeholder ready, backend schema ready)
- 🔜 Cross-platform installer (use jpackage after Maven build)

---

## LOCAL DEVELOPMENT (Two laptops on same WiFi)

### Step 1 — Prerequisites
- Java 21+ installed on both machines
- Maven installed (`mvn --version` to check)
- JavaFX SDK 21 downloaded from https://gluonhq.com/products/javafx/

### Step 2 — Build
```bash
mvn clean package -DskipTests
```

### Step 3 — Run Server (on ONE machine — the "host")
```bash
# Find your machine's local IP first
# Windows: ipconfig   Mac/Linux: ifconfig | grep "inet "

java -cp target/whatsup-3.0.0-jar-with-dependencies.jar server.ChatServer
# Server starts on port 25565
```

### Step 4 — Run Client (on BOTH machines)
```bash
# On the server machine (SERVER_HOST=localhost)
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls \
     -cp target/whatsup-3.0.0.jar App

# On the second machine (replace 192.168.1.x with server's IP)
SERVER_HOST=192.168.1.x java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls -cp target/whatsup-3.0.0.jar App
```

Both users register → login → they see each other in contacts → chat works!

---

## CLOUD DEPLOYMENT

### Option A — Railway (recommended for TCP socket server)

Railway supports custom TCP ports natively, perfect for your `ChatServer`.

1. Push project to GitHub:
   ```bash
   git init && git add . && git commit -m "Phase 3"
   git remote add origin https://github.com/YOUR_USERNAME/whatsup.git
   git push -u origin main
   ```

2. Go to https://railway.app → New Project → Deploy from GitHub → select repo

3. Add a **PostgreSQL** service in Railway:
   - Click "+ New" → Database → PostgreSQL
   - Copy the `DATABASE_URL` from the Variables tab

4. Set environment variables in Railway (Settings → Variables):
   ```
   DATABASE_URL    = (paste from PostgreSQL service)
   SERVER_PORT     = 25565
   SENDGRID_API_KEY = SG.xxxxxxxxxxxx
   SENDGRID_FROM    = yourverifiedemail@gmail.com
   ```

5. Set start command:
   ```
   java -cp target/whatsup-3.0.0-jar-with-dependencies.jar server.ChatServer
   ```

6. Railway gives you a URL like `whatsup.railway.app` — use the raw TCP host.

7. On client machines set:
   ```
   SERVER_HOST=whatsup.railway.app
   SERVER_PORT=25565
   ```

### Option B — Render (PostgreSQL only, server needs workaround)

**Important:** Render Web Services only support HTTP, NOT raw TCP sockets.
So on Render: deploy ONLY the database, run the ChatServer locally or on Railway.

**Setting up Render PostgreSQL:**
1. Go to https://render.com → New → PostgreSQL
2. Give it a name (e.g. `whatsup-db`) → Create
3. Copy the **Internal Database URL** (starts with `postgresql://`)
4. Add `jdbc:` prefix: `jdbc:postgresql://dpg-xxx.../whatsup`
5. Set this as `DATABASE_URL` env var wherever your server runs

**No code changes needed** — DBConnection.java auto-detects the env var.

---

## DATABASE

### Local — SQLite (default, zero config)
File `whatsup.db` is created automatically in the project folder.
**Limitation:** Wipes if you delete the file or redeploy to cloud.

### Cloud — PostgreSQL (persistent)
Set `DATABASE_URL=jdbc:postgresql://...` and DBConnection automatically switches.
`initSchema()` runs on every startup but only creates tables IF NOT EXISTS — safe.

**Schema:**
- `users` — phone (PK), full_name, email, profile_photo
- `messages` — private messages with media_path, media_type, is_delivered, is_read
- `groups` + `group_members` — group structure
- `group_messages` — group chat with media support

---

## SENDGRID OTP SETUP

1. Create free account at https://sendgrid.com (100 free emails/day)
2. Settings → API Keys → Create API Key → Mail Send permission
3. Settings → Sender Authentication → verify your email
4. Set env vars:
   ```
   SENDGRID_API_KEY=SG.your-key-here
   SENDGRID_FROM=verified@youremail.com
   ```

**Dev mode (no SendGrid):** Leave vars unset — OTP prints to console automatically.

---

## PROTOCOL REFERENCE (for evaluation)

```
Client → Server:
  LOGIN|phone
  PRIVATE|receiverPhone|content
  PRIVATE_MEDIA|receiverPhone|mediaType|base64Data|caption
  GROUP|groupId|content
  GROUP_MEDIA|groupId|mediaType|base64Data|caption

Server → Client:
  MSG|senderPhone|content|timestamp
  MSG_MEDIA|senderPhone|mediaType|base64Data|caption|timestamp
  GROUP_MSG|groupId|senderPhone|content|timestamp
  STATUS|OK  /  STATUS|ERROR|reason
```

---

## TEAM

| Member | Role | Phase 3 Contribution |
|--------|------|---------------------|
| Akshat Chand (Lead) | Server (ChatServer, ClientHandler, ChatClient) | Cloud deployment, Railway setup |
| Yash Negi | Group chat, pending messages | Group media sharing |
| Abhinav Baluni | UI (all screens) | Image sharing UI, read receipts |
| Rachit Nainwal | Database (all DAOs) | PostgreSQL migration, media schema |
