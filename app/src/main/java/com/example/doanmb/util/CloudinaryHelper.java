package com.example.doanmb.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloudinaryHelper {
    private static final String UPLOAD_PRESET = "doanmb_preset";
    private static final int MAX_SIDE = 1024; 

    public interface OnUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void uploadImage(Context context, Uri imageUri, OnUploadCallback callback) {
        new Thread(() -> {
            File tempFile = null;
            try {
                tempFile = compressAndRotateImage(context, imageUri);
                if (tempFile == null) {
                    callback.onFailure("Lỗi xử lý tệp ảnh");
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
                callback.onFailure("Lỗi hệ thống: " + e.getMessage());
            }
        }).start();
    }

    private static File compressAndRotateImage(Context context, Uri uri) throws IOException {
        // 1. Xác định độ xoay của ảnh từ EXIF
        int rotation = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: rotation = 90; break;
                    case ExifInterface.ORIENTATION_ROTATE_180: rotation = 180; break;
                    case ExifInterface.ORIENTATION_ROTATE_270: rotation = 270; break;
                }
            }
        }

        // 2. Đọc kích thước ảnh
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }

        int sampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / sampleSize > MAX_SIDE) {
            sampleSize *= 2;
        }

        // 3. Giải mã Bitmap vào bộ nhớ
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }

        if (bitmap == null) return null;

        // 4. Xoay ảnh về đúng chiều dọc nếu cần
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotated;
        }

        // 5. Lưu ra file tạm thời để upload
        File file = File.createTempFile("upload_", ".jpg", context.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } finally {
            bitmap.recycle();
        }
        return file;
    }
}
