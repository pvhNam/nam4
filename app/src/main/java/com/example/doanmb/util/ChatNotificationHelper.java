package com.example.doanmb.util;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper gửi thông báo chat qua FCM V1 API (không cần Cloud Function, không cần Blaze plan).
 *
 * Cách hoạt động:
 * 1. Đọc service-account.json từ assets để lấy private_key + client_email
 * 2. Tự tạo JWT token để xác thực với Google OAuth2
 * 3. Đổi JWT lấy Access Token
 * 4. Gọi FCM V1 API để gửi notification đến thiết bị người nhận
 */
public final class ChatNotificationHelper {

    private static final String TAG         = "ChatNotifHelper";
    private static final String PROJECT_ID  = "doanmb-a73a9";
    private static final String FCM_URL     =
            "https://fcm.googleapis.com/v1/projects/" + PROJECT_ID + "/messages:send";
    private static final String TOKEN_URL   = "https://oauth2.googleapis.com/token";
    private static final String SCOPE       = "https://www.googleapis.com/auth/firebase.messaging";

    // Cache access token (hết hạn sau ~1 tiếng, tự refresh)
    private static String  cachedAccessToken     = null;
    private static long    tokenExpiryTimeMillis  = 0;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private ChatNotificationHelper() {}

    // ─────────────────────────────────────────────────────────────────────────
    // API CHÍNH (có carType)
    // ─────────────────────────────────────────────────────────────────────────

    public static void sendChatNotification(Context context,
                                            String receiverId,
                                            String senderId,
                                            String senderName,
                                            String carName,
                                            String carType,
                                            String messagePreview,
                                            String roomId) {
        if (receiverId == null || receiverId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String title = buildTitle(senderName);
        String body  = buildBody(senderName, carName, carType, messagePreview);

        // ── 1. Lưu vào Firestore "notifications" (tab Thông báo trong app) ──
        // ── Document ID cố định = "receiverId_roomId"
        // → mỗi cuộc trò chuyện chỉ có 1 thông báo duy nhất,
        //   tin nhắn mới sẽ cập nhật (upsert) thay vì tạo mới
        String notifDocId = receiverId + "_" + (roomId != null ? roomId : "");

        Map<String, Object> notif = new HashMap<>();
        notif.put("userId",     receiverId);
        notif.put("senderId",   senderId);
        notif.put("title",      title);
        notif.put("body",       body);
        notif.put("type",       "chat");
        notif.put("roomId",     roomId     != null ? roomId     : "");
        notif.put("carName",    carName    != null ? carName    : "");
        notif.put("carType",    carType    != null ? carType    : "sale");
        notif.put("senderName", senderName != null ? senderName : "");
        notif.put("read",       false);       // reset về chưa đọc mỗi khi có tin mới
        notif.put("createdAt",  Timestamp.now());

        // set() với merge=false → tạo mới nếu chưa có, ghi đè nếu đã có
        db.collection("notifications").document(notifDocId).set(notif)
                .addOnSuccessListener(v -> Log.d(TAG, "Notification upserted: " + notifDocId))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to upsert notification", e));

        // ── 2. Lấy FCM token của người nhận → gọi FCM V1 API ────────────────
        final String finalTitle = title;
        final String finalBody  = body;

        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String fcmToken = doc.getString("fcmToken");
                    if (fcmToken == null || fcmToken.isEmpty()) {
                        Log.d(TAG, "No FCM token for: " + receiverId);
                        return;
                    }
                    executor.execute(() ->
                            sendFcmV1(context, fcmToken, finalTitle, finalBody,
                                    senderName, carName, carType, roomId, senderId));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to get FCM token", e));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BACKWARD-COMPAT — BẮT BUỘC truyền Context, không dùng null
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @deprecated Luôn dùng overload có Context để FCM hoạt động trên điện thoại thật.
     */
    @Deprecated
    public static void sendChatNotification(String receiverId,
                                            String senderId,
                                            String senderName,
                                            String carName,
                                            String carType,
                                            String messagePreview,
                                            String roomId) {
        throw new IllegalStateException(
                "Phải truyền Context vào sendChatNotification. " +
                        "Dùng overload: sendChatNotification(context, receiverId, ...)");
    }

    /**
     * @deprecated Luôn dùng overload có Context để FCM hoạt động trên điện thoại thật.
     */
    @Deprecated
    public static void sendChatNotification(String receiverId,
                                            String senderId,
                                            String senderName,
                                            String carName,
                                            String messagePreview,
                                            String roomId) {
        throw new IllegalStateException(
                "Phải truyền Context vào sendChatNotification. " +
                        "Dùng overload: sendChatNotification(context, receiverId, ...)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FCM V1 API — gọi từ background thread
    // ─────────────────────────────────────────────────────────────────────────

    private static void sendFcmV1(Context context,
                                  String deviceToken,
                                  String title, String body,
                                  String senderName, String carName, String carType,
                                  String roomId, String senderId) {
        try {
            // 1. Lấy access token (cache 55 phút)
            String accessToken = getAccessToken(context);
            if (accessToken == null) {
                Log.e(TAG, "Không lấy được access token");
                return;
            }

            // 2. Build FCM payload
            JSONObject dataObj = new JSONObject();
            dataObj.put("title",      title);
            dataObj.put("body",       body);
            dataObj.put("senderName", senderName != null ? senderName : "");
            dataObj.put("carName",    carName    != null ? carName    : "");
            dataObj.put("carType",    carType    != null ? carType    : "sale");
            dataObj.put("roomId",     roomId     != null ? roomId     : "");
            dataObj.put("senderId",   senderId   != null ? senderId   : "");

            JSONObject androidConfig = new JSONObject();
            androidConfig.put("priority", "high");

            JSONObject messageObj = new JSONObject();
            messageObj.put("token",   deviceToken);
            messageObj.put("data",    dataObj);
            messageObj.put("android", androidConfig);

            JSONObject payload = new JSONObject();
            payload.put("message", messageObj);

            // 3. Gửi HTTP POST
            URL url = new URL(FCM_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                Log.d(TAG, "FCM V1 gửi thành công!");
            } else {
                InputStream err = conn.getErrorStream();
                String errBody = err != null ? new BufferedReader(new InputStreamReader(err))
                        .lines().reduce("", (a, b) -> a + b) : "";
                Log.w(TAG, "FCM V1 lỗi " + code + ": " + errBody);
            }
            conn.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "sendFcmV1 error: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LẤY OAUTH2 ACCESS TOKEN TỪ SERVICE ACCOUNT JSON
    // ─────────────────────────────────────────────────────────────────────────

    private static synchronized String getAccessToken(Context context) {
        // Dùng cache nếu còn hạn (trừ 5 phút để an toàn)
        if (cachedAccessToken != null &&
                System.currentTimeMillis() < tokenExpiryTimeMillis - 5 * 60 * 1000) {
            return cachedAccessToken;
        }

        try {
            // Đọc service-account.json từ assets
            Context appContext = context != null
                    ? context.getApplicationContext()
                    : null;
            if (appContext == null) {
                Log.e(TAG, "Context null, không đọc được service-account.json");
                return null;
            }

            InputStream is = appContext.getAssets().open("service-account.json");
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            is.close();

            JSONObject sa          = new JSONObject(sb.toString());
            String     clientEmail = sa.getString("client_email");
            String     privateKeyPem = sa.getString("private_key");

            // Parse private key
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);

            // Tạo JWT
            String jwt = buildJwt(clientEmail, privateKey);

            // Đổi JWT lấy access token
            String accessToken = exchangeJwtForToken(jwt);
            if (accessToken != null) {
                cachedAccessToken    = accessToken;
                tokenExpiryTimeMillis = System.currentTimeMillis() + 3600 * 1000; // 1 tiếng
            }
            return accessToken;

        } catch (Exception e) {
            Log.e(TAG, "getAccessToken error: " + e.getMessage(), e);
            return null;
        }
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        String clean = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(clean);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static String buildJwt(String clientEmail, PrivateKey key) throws Exception {
        long now = System.currentTimeMillis() / 1000;

        // Header
        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        String headerB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.toString().getBytes(StandardCharsets.UTF_8));

        // Claims
        JSONObject claims = new JSONObject();
        claims.put("iss",   clientEmail);
        claims.put("scope", SCOPE);
        claims.put("aud",   TOKEN_URL);
        claims.put("iat",   now);
        claims.put("exp",   now + 3600);
        String claimsB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8));

        // Sign
        String signingInput = headerB64 + "." + claimsB64;
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String sigB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sig.sign());

        return signingInput + "." + sigB64;
    }

    private static String exchangeJwtForToken(String jwt) throws Exception {
        String body = "grant_type=" +
                java.net.URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                "&assertion=" + java.net.URLEncoder.encode(jwt, "UTF-8");

        URL url = new URL(TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            JSONObject json = new JSONObject(response.toString());
            return json.getString("access_token");
        } else {
            Log.e(TAG, "exchangeJwt failed: " + code);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public static String buildTitle(String senderName) {
        if (senderName != null && !senderName.isEmpty())
            return "Tin nhắn từ " + senderName;
        return "Tin nhắn mới";
    }

    /** @deprecated Dùng buildTitle(senderName) */
    public static String buildTitle(String senderName, String carName) {
        return buildTitle(senderName);
    }

    public static String buildBody(String senderName, String carName,
                                   String carType, String messagePreview) {
        String who = (senderName != null && !senderName.isEmpty()) ? senderName : "Ai đó";
        if (carName != null && !carName.isEmpty()) {
            String action = "rental".equalsIgnoreCase(carType) ? "thuê" : "mua";
            return who + " muốn " + action + " xe " + carName;
        }
        if (messagePreview != null && !messagePreview.isEmpty()) {
            return messagePreview.length() > 70
                    ? messagePreview.substring(0, 70) + "…"
                    : messagePreview;
        }
        return "Bạn có tin nhắn mới";
    }

    public static String buildBody(String senderName, String carName, String messagePreview) {
        return buildBody(senderName, carName, "sale", messagePreview);
    }
}