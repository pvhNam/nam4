package com.example.doanmb.util;

import android.content.Context;
import android.net.Uri;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloudinaryHelper {

    private static final String UPLOAD_PRESET = "doanmb_preset";

    public interface OnUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    // Giới hạn upload
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "image/webp"};

    public static void uploadImage(Context context, Uri imageUri, OnUploadCallback callback) {
        try {
            // Kiểm tra định dạng file
            String mimeType = context.getContentResolver().getType(imageUri);
            boolean isAllowed = false;
            for (String allowed : ALLOWED_MIME_TYPES) {
                if (allowed.equals(mimeType)) { isAllowed = true; break; }
            }
            if (!isAllowed) {
                callback.onFailure("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP!");
                return;
            }

            // Đọc trực tiếp dữ liệu ảnh thành byte[] ngay tại chỗ để giữ quyền truy cập file
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onFailure("Không thể mở hoặc đọc file ảnh!");
                return;
            }
            byte[] bytes = getBytes(inputStream);

            // Kiểm tra kích thước file
            if (bytes.length > MAX_FILE_SIZE_BYTES) {
                callback.onFailure("Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB.");
                return;
            }

            // Tiến hành upload bằng byte[] thay vì truyền Uri thô
            MediaManager.get().upload(bytes)
                    .unsigned(UPLOAD_PRESET)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, java.util.Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            callback.onSuccess(url != null ? url : "");
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            callback.onFailure(error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            callback.onFailure("Reschedule: " + error.getDescription());
                        }
                    })
                    .dispatch(context);

        } catch (IOException e) {
            callback.onFailure("Lỗi xử lý luồng dữ liệu ảnh: " + e.getMessage());
        } catch (Exception e) {
            callback.onFailure("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Hàm phụ trợ chuyển đổi luồng dữ liệu sang mảng byte
    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}