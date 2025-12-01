package com.example.smartairsetup;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TechniqueTrainingVideo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_technique_training_video);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setUpVideo();
        setBackButton();

    }

    private void setUpVideo() {
        VideoView videoView = findViewById(R.id.techniqueVV);

        //add playback controls so user can pause and navigate video
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // Build the URI for the local raw file
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.inhaler_technique);

        // Set and play
        videoView.setVideoURI(videoUri);
        videoView.requestFocus();
        videoView.start();

    }
    private void setBackButton() {
        Button backButton = findViewById(R.id.videoBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }
    }


}