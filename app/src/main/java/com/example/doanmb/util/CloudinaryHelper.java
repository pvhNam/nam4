package com.example.doanmb.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloudinaryHelper {
    private static final String UPLOAD_PRESET = "doanmb_preset";
    private static final int MAX_SIDE = 800; // Chat chỉ cần 800px là cực rõ và cực nhanh

    public interface OnUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void uploadImage(Context context, Uri imageUri, OnUploadCallback callback) {
        new Thread(() -> {
            File tempFile = null;
            try {
                tempFile = compressImageSafely(context, imageUri);
                if (tempFile == null) {
                    callback.onFailure("Không thể xử lý tệp ảnh");
                    return;
                }

                final File fileToUpload = tempFile;
                MediaManager.get().upload(fileToUpload.getAbsolutePath())
                        .unsigned(UPLOAD_PRESET)
                        .option("resource_type", "image")
                        .callback(new UploadCallback() {
                            @Override public void onStart(String requestId) {}
                            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                            @Override
                            public void onSuccess(String requestId, java.util.Map resultData) {
                                fileToUpload.delete();
                                callback.onSuccess((String) resultData.get("secure_url"));
                            }
                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                fileToUpload.delete();
                                callback.onFailure(error.getDescription());
                            }
                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {}
                        }).dispatch(context);
            } catch (Exception e) {
                if (tempFile != null) tempFile.delete();
                callback.onFailure("Lỗi RAM: " + e.getMessage());
            }
        }).start();
    }

    private static File compressImageSafely(Context context, Uri uri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }

        int sampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / sampleSize > MAX_SIDE) {
            sampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Tiết kiệm 50% RAM so với mặc định

        Bitmap bitmap = null;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }

        if (bitmap == null) return null;

        File file = File.createTempFile("chat_img", ".jpg", context.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
            out.flush();
        } finally {
            bitmap.recycle(); // Giải phóng RAM ngay lập tức
            System.gc(); // Gọi dọn rác hệ thống
        }
        return file;
    }
}
