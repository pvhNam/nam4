package com.example.doanmb.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;

/**
 * Xem ảnh / video toàn màn hình, vuốt qua lại giữa các media.
 *
 * Extras:
 *   EXTRA_URLS       ArrayList<String>  — danh sách URL
 *   EXTRA_IS_VIDEOS  ArrayList<Boolean> — tương ứng là video hay ảnh
 *   EXTRA_START_POS  int                — vị trí bắt đầu
 */
public class FullscreenMediaActivity extends AppCompatActivity {

    public static final String EXTRA_URLS      = "MEDIA_URLS";
    public static final String EXTRA_IS_VIDEOS = "MEDIA_IS_VIDEOS";
    public static final String EXTRA_START_POS = "MEDIA_START_POS";

    private ViewPager2   viewPager;
    private TextView     tvCounter;
    private ImageView    btnClose;

    private ArrayList<String>  urls;
    private ArrayList<Boolean> isVideos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fullscreen_media);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        urls      = getIntent().getStringArrayListExtra(EXTRA_URLS);
        isVideos  = (ArrayList<Boolean>) getIntent().getSerializableExtra(EXTRA_IS_VIDEOS);
        int start = getIntent().getIntExtra(EXTRA_START_POS, 0);

        if (urls == null || urls.isEmpty()) { finish(); return; }
        if (isVideos == null) isVideos = new ArrayList<>();

        viewPager = findViewById(R.id.viewpager_media);
        tvCounter = findViewById(R.id.tv_media_counter);
        btnClose  = findViewById(R.id.btn_close_media);

        btnClose.setOnClickListener(v -> finish());

        viewPager.setAdapter(new MediaPagerAdapter());
        viewPager.setCurrentItem(start, false);
        updateCounter(start);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounter(position);
            }
        });
    }

    private void updateCounter(int pos) {
        if (urls.size() <= 1) {
            tvCounter.setVisibility(View.GONE);
        } else {
            tvCounter.setVisibility(View.VISIBLE);
            tvCounter.setText((pos + 1) + " / " + urls.size());
        }
    }

    // ── PagerAdapter ─────────────────────────────────────────────────────────

    private class MediaPagerAdapter
            extends androidx.recyclerview.widget.RecyclerView.Adapter<MediaPagerAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_media_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String  url     = urls.get(position);
            boolean isVideo = position < isVideos.size() && Boolean.TRUE.equals(isVideos.get(position));

            if (isVideo) {
                // Hiện video player, ẩn photoview
                h.photoView.setVisibility(View.GONE);
                h.videoView.setVisibility(View.VISIBLE);
                h.progressBar.setVisibility(View.VISIBLE);
                h.ivPlayBtn.setVisibility(View.VISIBLE);

                h.videoView.setVideoURI(Uri.parse(url));
                MediaController mc = new MediaController(FullscreenMediaActivity.this);
                mc.setAnchorView(h.videoView);
                h.videoView.setMediaController(mc);

                h.videoView.setOnPreparedListener(mp -> {
                    h.progressBar.setVisibility(View.GONE);
                    h.ivPlayBtn.setVisibility(View.GONE);
                    mp.setLooping(false);
                });
                h.videoView.setOnErrorListener((mp, w, e) -> {
                    h.progressBar.setVisibility(View.GONE);
                    Toast.makeText(FullscreenMediaActivity.this,
                            "Không thể phát video", Toast.LENGTH_SHORT).show();
                    return true;
                });

                // Bấm vào play button thì bắt đầu phát
                h.ivPlayBtn.setOnClickListener(v -> {
                    h.ivPlayBtn.setVisibility(View.GONE);
                    h.videoView.start();
                });

            } else {
                // Hiện ảnh với zoom, ẩn video
                h.videoView.setVisibility(View.GONE);
                h.progressBar.setVisibility(View.GONE);
                h.ivPlayBtn.setVisibility(View.GONE);
                h.photoView.setVisibility(View.VISIBLE);

                String optimized = url.contains("cloudinary.com")
                        ? url.replaceAll("/upload/[^/]+/", "/upload/")
                        : url;

                Glide.with(h.photoView.getContext())
                        .load(optimized)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(h.photoView);
            }
        }

        @Override
        public int getItemCount() { return urls.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            PhotoView   photoView;
            VideoView   videoView;
            ProgressBar progressBar;
            ImageView   ivPlayBtn;

            VH(@NonNull View v) {
                super(v);
                photoView   = v.findViewById(R.id.photo_view_page);
                videoView   = v.findViewById(R.id.video_view_page);
                progressBar = v.findViewById(R.id.progress_page);
                ivPlayBtn   = v.findViewById(R.id.iv_play_page);
            }
        }
    }
}