package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class ParentHomeActivity extends AppCompatActivity {

    private ImageButton familyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        familyButton = findViewById(R.id.familyButton);

        familyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentHomeActivity.this, ParentFamilyActivity.class);
                startActivity(intent);
            }
        });
    }

}
