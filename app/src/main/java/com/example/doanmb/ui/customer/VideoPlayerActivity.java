package com.example.doanmb.ui.customer;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmb.databinding.ActivityVideoPlayerBinding;

/** Phát video trong app. Màn thuần hiển thị nên không cần ViewModel. */
public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "VIDEO_URL";

    private ActivityVideoPlayerBinding binding;
    private int savedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        binding.btnCloseVideo.setOnClickListener(v -> finish());

        String videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy video", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupVideoPlayer(videoUrl);
    }

    private void setupVideoPlayer(String url) {
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(binding.videoView);
        binding.videoView.setMediaController(mediaController);
        binding.videoView.setVideoURI(Uri.parse(url));

        binding.progressVideo.setVisibility(View.VISIBLE);
        binding.videoView.setOnPreparedListener(mp -> {
            binding.progressVideo.setVisibility(View.GONE);
            mp.setLooping(false);
            if (savedPosition > 0) binding.videoView.seekTo(savedPosition);
            binding.videoView.start();
        });
        binding.videoView.setOnErrorListener((mp, what, extra) -> {
            binding.progressVideo.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể phát video này", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
        binding.videoView.setOnCompletionListener(mp -> savedPosition = 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding.videoView.isPlaying()) {
            savedPosition = binding.videoView.getCurrentPosition();
            binding.videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (savedPosition > 0) {
            binding.videoView.seekTo(savedPosition);
            binding.videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.videoView.stopPlayback();
    }
}
