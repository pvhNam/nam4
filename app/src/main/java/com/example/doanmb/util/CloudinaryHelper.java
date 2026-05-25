package com.example.doanmb.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    // Cấu hình giới hạn nén ảnh để tối ưu băng thông
    private static final int MAX_IMAGE_DIMENSION = 2048; // Kích thước cạnh dài tối đa (Pixel)
    private static final int COMPRESS_QUALITY = 80;      // Chất lượng nén 80% (Cân bằng tốt giữa độ nét và dung lượng)

    public interface OnUploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public static void uploadImage(Context context, Uri imageUri, OnUploadCallback callback) {
        new Thread(() -> {
            File compressedFile = null;
            try {
                // Thực hiện giải mã, resize và nén trực tiếp từ Uri sang một file JPEG tạm thời
                compressedFile = decodeAndCompressImage(context, imageUri);
                if (compressedFile == null || !compressedFile.exists()) {
                    callback.onFailure("Không thể giải mã hoặc nén tập tin ảnh này!");
                    return;
                }

                final File finalFileToUpload = compressedFile;

                // Đẩy file đã được nén tối ưu lên Cloudinary
                MediaManager.get().upload(finalFileToUpload.getAbsolutePath())
                        .unsigned(UPLOAD_PRESET)
                        .option("resource_type", "image") // Lúc này file đã là định dạng ảnh JPG tiêu chuẩn
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {}

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {}

                            @Override
                            public void onSuccess(String requestId, java.util.Map resultData) {
                                deleteQuietly(finalFileToUpload);
                                String url = (String) resultData.get("secure_url");
                                callback.onSuccess(url != null ? url : "");
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                deleteQuietly(finalFileToUpload);
                                callback.onFailure("Cloudinary: " + error.getDescription());
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {
                                deleteQuietly(finalFileToUpload);
                                callback.onFailure("Reschedule: " + error.getDescription());
                            }
                        })
                        .dispatch(context);

            } catch (Exception e) {
                if (compressedFile != null) deleteQuietly(compressedFile);
                callback.onFailure("Lỗi hệ thống khi xử lý nén: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Hàm giải mã ảnh (hỗ trợ đọc lướt kích thước để tránh OOM), thực hiện scale nhỏ và nén chất lượng thành file JPEG.
     */
    private static File decodeAndCompressImage(Context context, Uri uri) throws IOException {
        // Bước 1: Chỉ đọc thông số kích thước (Bounds) của ảnh gốc chứ chưa load toàn bộ điểm ảnh vào RAM
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }

        // Bước 2: Tính toán tỷ lệ thu nhỏ (Sample Size) nếu ảnh vượt quá MAX_IMAGE_DIMENSION
        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;
        int inSampleSize = 1;

        if (srcWidth > MAX_IMAGE_DIMENSION || srcHeight > MAX_IMAGE_DIMENSION) {
            final int halfHeight = srcHeight / 2;
            final int halfWidth = srcWidth / 2;
            while ((halfHeight / inSampleSize) >= MAX_IMAGE_DIMENSION && (halfWidth / inSampleSize) >= MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2;
            }
        }

        // Bước 3: Giải mã thực tế luồng ảnh vào RAM theo tỷ lệ thu nhỏ đã tính toán (Chống sập RAM)
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        Bitmap srcBitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            srcBitmap = BitmapFactory.decodeStream(input, null, options);
        }

        if (srcBitmap == null) return null;

        // Bước 4: Tạo file tạm thời và ghi dữ liệu đã nén chất lượng bằng cấu trúc JPEG tiêu chuẩn
        File tempFile = File.createTempFile("compressed_img_", ".jpg", context.getCacheDir());
        try (FileOutputStream outStream = new FileOutputStream(tempFile)) {
            // Nén Bitmap trực tiếp xuống ổ đĩa với định dạng JPEG và chất lượng định sẵn
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outStream);
            outStream.flush();
        } finally {
            srcBitmap.recycle(); // Giải phóng vùng nhớ Bitmap ngay lập tức sau khi dùng xong
        }

        return tempFile;
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}