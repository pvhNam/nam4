/**
 * Firebase Cloud Functions – Carvia Push Notification
 *
 * Khi ChatNotificationHelper thêm doc vào "fcm_queue",
 * function này tự động trigger và gọi FCM API để đẩy
 * heads-up notification đến điện thoại người nhận
 * (kể cả khi app đang tắt hoặc đang dùng điện thoại mà không mở app).
 *
 * Setup:
 *   1. cd functions && npm install
 *   2. firebase deploy --only functions
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp }      = require("firebase-admin/app");
const { getMessaging }       = require("firebase-admin/messaging");
const { getFirestore }       = require("firebase-admin/firestore");

initializeApp();

/**
 * Trigger: mỗi khi có doc mới trong collection "fcm_queue"
 * Doc có dạng:
 * {
 *   token:      "FCM_TOKEN_CUA_NGUOI_NHAN",
 *   title:      "Tin nhắn từ Nguyễn Văn A",
 *   body:       "Nguyễn Văn A muốn mua xe Toyota Camry",
 *   senderName: "Nguyễn Văn A",
 *   carName:    "Toyota Camry",
 *   carType:    "sale" | "rental",
 *   roomId:     "ROOM_ID",
 *   senderId:   "SENDER_UID",
 *   createdAt:  Timestamp,
 *   sent:       false   ← Cloud Function set true sau khi gửi
 * }
 */
exports.sendChatPushNotification = onDocumentCreated(
  "fcm_queue/{docId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data  = snap.data();
    const docId = event.params.docId;

    // Bỏ qua nếu đã gửi rồi (tránh gửi 2 lần)
    if (data.sent === true) return;

    const token = data.token;
    if (!token) {
      console.warn(`[${docId}] Không có FCM token, bỏ qua.`);
      await snap.ref.update({ sent: true, error: "no_token" });
      return;
    }

    // ── Tạo payload FCM ──────────────────────────────────────────────────────
    // Dùng "data" message (không dùng "notification" field) để
    // CarviaMessagingService xử lý và hiển thị heads-up đúng cách.
    const message = {
      token: token,
      data: {
        title:      data.title      || "Tin nhắn mới",
        body:       data.body       || "Bạn có tin nhắn mới",
        senderName: data.senderName || "",
        carName:    data.carName    || "",
        carType:    data.carType    || "sale",
        roomId:     data.roomId     || "",
        senderId:   data.senderId   || "",
      },
      android: {
        priority: "high",   // Đảm bảo heads-up hoạt động kể cả Doze mode
      },
      apns: {               // iOS (nếu sau này mở rộng)
        headers: {
          "apns-priority": "10",
        },
      },
    };

    // ── Gửi FCM ──────────────────────────────────────────────────────────────
    try {
      const response = await getMessaging().send(message);
      console.log(`[${docId}] FCM gửi thành công:`, response);

      // Đánh dấu đã gửi
      await snap.ref.update({ sent: true, sentAt: new Date() });
    } catch (err) {
      console.error(`[${docId}] FCM gửi thất bại:`, err.message);

      // Token hết hạn → xoá khỏi user document
      if (
        err.code === "messaging/registration-token-not-registered" ||
        err.code === "messaging/invalid-registration-token"
      ) {
        await cleanupInvalidToken(data.token);
      }

      await snap.ref.update({ sent: true, error: err.message });
    }
  }
);

/**
 * Xoá FCM token hết hạn khỏi Firestore để tránh spam lần sau.
 */
async function cleanupInvalidToken(token) {
  try {
    const db      = getFirestore();
    const usersRef = db.collection("users");
    const snapshot = await usersRef.where("fcmToken", "==", token).get();
    for (const doc of snapshot.docs) {
      await doc.ref.update({ fcmToken: "" });
      console.log(`Đã xoá token hết hạn của user: ${doc.id}`);
    }
  } catch (e) {
    console.error("cleanupInvalidToken error:", e.message);
  }
}