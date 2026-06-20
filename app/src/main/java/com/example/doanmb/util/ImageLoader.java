package com.example.doanmb.util;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

/**
 * Tải ảnh tối ưu dùng chung toàn app:
 * - Resize qua Cloudinary (w_, q_auto, f_auto) để giảm dung lượng tải → nhanh hơn.
 * - Cache đĩa (DiskCacheStrategy.ALL) để lần sau hiện tức thì.
 * - RGB_565 cho ảnh nhỏ (card/avatar) để nhẹ RAM, cuộn mượt, ít giật.
 */
public final class ImageLoader {

    private ImageLoader() {}

    private static final int THUMB_W  = 600;   // ảnh card/danh sách
    private static final int DETAIL_W = 1080;  // ảnh chi tiết/gallery
    private static final int AVATAR_W = 240;   // avatar tròn

    /** Ảnh card/danh sách (cắt đầy khung, nhẹ RAM). */
    public static void loadCard(ImageView iv, String url, int placeholderRes) {
        lite(iv, url, THUMB_W, placeholderRes).centerCrop().into(iv);
    }

    /** Ảnh chi tiết/gallery (giữ chất lượng cao hơn, theo scaleType của ImageView). */
    public static void loadDetail(ImageView iv, String url, int placeholderRes) {
        RequestBuilder<Drawable> rb = Glide.with(iv.getContext())
                .load(CloudinaryHelper.optimizeImageUrl(url, DETAIL_W))
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        if (placeholderRes != 0) rb = rb.placeholder(placeholderRes);
        rb.into(iv);
    }

    /** Avatar tròn (có ảnh chờ). */
    public static void loadAvatar(ImageView iv, String url, int placeholderRes) {
        lite(iv, url, AVATAR_W, placeholderRes).circleCrop().into(iv);
    }

    /** Avatar tròn (không cần ảnh chờ). */
    public static void loadAvatar(ImageView iv, String url) {
        lite(iv, url, AVATAR_W, 0).circleCrop().into(iv);
    }

    private static RequestBuilder<Drawable> lite(ImageView iv, String url, int width, int placeholderRes) {
        RequestBuilder<Drawable> rb = Glide.with(iv.getContext())
                .load(CloudinaryHelper.optimizeImageUrl(url, width))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .format(DecodeFormat.PREFER_RGB_565);
        if (placeholderRes != 0) rb = rb.placeholder(placeholderRes).error(placeholderRes);
        return rb;
    }
}
