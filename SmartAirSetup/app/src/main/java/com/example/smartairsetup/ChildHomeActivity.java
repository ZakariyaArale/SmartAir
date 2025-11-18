
package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.example.smartairsetup.ParentFamilyActivity;
import com.example.smartairsetup.R;

public class ChildHomeActivity extends AppCompatActivity {

    private ImageButton familyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        familyButton = findViewById(R.id.familyButton);

        familyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildHomeActivity.this, ChildFamilyActivity.class);
                startActivity(intent);
            }
        });
    }

}
