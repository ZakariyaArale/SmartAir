package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TEMP: reuse main layout so the project compiles.
        setContentView(R.layout.activity_main);
    }
}
