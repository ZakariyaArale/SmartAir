package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class ParentFamilyActivity extends AppCompatActivity {
    private ImageButton homeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_family);

        homeButton = findViewById(R.id.homeButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentFamilyActivity.this, ParentHomeActivity.class);

                startActivity(intent);
            }
        });
    }

}
