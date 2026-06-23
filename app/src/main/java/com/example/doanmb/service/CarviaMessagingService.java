package com.example.doanmb.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.doanmb.MainActivity;
import com.example.doanmb.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý FCM push notification cho tính năng chat.
 * Khi có tin nhắn mới từ người khác → đẩy thông báo dạng "heads-up"
 * (nổi lên giống Messenger) khi app đang tắt hoặc ở background.
 *
 * Đăng ký trong AndroidManifest:
 * <service android:name=".service.CarviaMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT"/>
 *     </intent-filter>
 * </service>
 */
public class CarviaMessagingService extends FirebaseMessagingService {

    private static final String TAG          = "CarviaMsgSvc";
    private static final String CHANNEL_CHAT = "carvia_chat";
    private static final String CHANNEL_NAME = "Tin nhắn";
    private static final int    NOTIF_ID     = 1001;

    // ── Nhận tin nhắn FCM ────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) return;

        String title      = data.getOrDefault("title",      "Tin nhắn mới");
        String body       = data.getOrDefault("body",       "");
        String senderName = data.getOrDefault("senderName", "");
        String roomId     = data.getOrDefault("roomId",     "");
        String carName    = data.getOrDefault("carName",    "");
        String partnerId  = data.getOrDefault("senderId",   "");

        // Không hiện thông báo nếu đang đứng trong chính phòng chat đó
        // (Kiểm tra sẽ được ChatDetailActivity tự handle — ở đây cứ show)
        showChatNotification(title, body, senderName, carName, roomId, partnerId);
    }

    // ── FCM token mới → lưu lên Firestore ───────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        saveFcmToken(token);
    }

    // ── Hiển thị notification dạng heads-up ─────────────────────────────────

    private void showChatNotification(String title, String body,
                                      String senderName, String carName,
                                      String roomId, String partnerId) {
        createNotificationChannel();

        // Khi bấm vào thông báo → mở MainActivity (tab Messages)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("OPEN_TAB", "messages");
        intent.putExtra("ROOM_ID",  roomId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Nội dung thông báo: nếu có tên xe → hiện "về xe X"
        String contentTitle = senderName.isEmpty() ? title : senderName;
        String contentText  = body;
        if (!carName.isEmpty() && !body.contains(carName)) {
            contentText = body + " (xe: " + carName + ")";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_CHAT)
                .setSmallIcon(R.mipmap.ic_launcher)                // icon app nhỏ góc trên
                .setLargeIcon(BitmapFactory.decodeResource(
                        getResources(), R.mipmap.ic_launcher_round)) // icon tròn lớn
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)      // heads-up priority
                .setDefaults(NotificationCompat.DEFAULT_ALL)        // âm thanh + rung
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                // Heads-up (nổi lên khi đang dùng điện thoại) cần PRIORITY_HIGH + channel HIGH
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        try {
            // Dùng roomId hash làm notifId để gộp nhiều tin cùng phòng
            int notifId = roomId.isEmpty() ? NOTIF_ID : (NOTIF_ID + roomId.hashCode() % 1000);
            manager.notify(notifId, builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission denied: " + e.getMessage());
        }
    }

    // ── Tạo channel (Android 8+) ─────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_CHAT,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH); // HIGH = heads-up
            channel.setDescription("Thông báo tin nhắn Carvia");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ── Lưu FCM token vào Firestore ─────────────────────────────────────────

    public static void saveFcmToken(Context context, String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(data)
                .addOnFailureListener(e -> Log.w("CarviaMsgSvc", "saveFcmToken failed", e));
    }

    private void saveFcmToken(String token) {
        saveFcmToken(this, token);
    }
}