package com.example.smartairsetup.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
import com.example.smartairsetup.provider_home_ui.ProviderHomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText signupEmail;
    private EditText signupPassword;
    private RadioGroup radioGroupRole;
    private Button buttonSignup;
    private TextView signupError;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button backButton1 = findViewById(R.id.backButton);
        backButton1.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        buttonSignup = findViewById(R.id.buttonSignup);
        signupError = findViewById(R.id.signupError);

        buttonSignup.setOnClickListener(v -> handleSignup());
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            // Avoid stacking multiple copies of Welcome
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private String getSelectedRole() {
        int checkedId = radioGroupRole.getCheckedRadioButtonId();
        if (checkedId == R.id.radioParent) return "parent";
        if (checkedId == R.id.radioProvider) return "provider";
        return null;
    }

    private void handleSignup() {
        signupError.setVisibility(View.GONE);
        signupError.setText("");

        buttonSignup.setEnabled(false);

        String email = signupEmail.getText().toString().trim().toLowerCase();
        String password = signupPassword.getText().toString();
        String role = getSelectedRole();

        if (TextUtils.isEmpty(email)) {
            signupEmail.setError("Email is required");
            signupEmail.requestFocus();
            buttonSignup.setEnabled(true);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Enter a valid email address");
            signupEmail.requestFocus();
            buttonSignup.setEnabled(true);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            signupPassword.setError("Password is required");
            signupPassword.requestFocus();
            buttonSignup.setEnabled(true);
            return;
        }

        if (password.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            signupPassword.requestFocus();
            buttonSignup.setEnabled(true);
            return;
        }

        if (role == null) {
            signupError.setText("Please select whether you are a Parent or Provider");
            signupError.setVisibility(View.VISIBLE);
            buttonSignup.setEnabled(true);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        saveUserDocument(uid, email, role);

                        Toast.makeText(SignupActivity.this,
                                "Account created!",
                                Toast.LENGTH_SHORT).show();

                        buttonSignup.setEnabled(true);
                        goToRoleHome(role);
                    } else {
                        buttonSignup.setEnabled(true);
                        String message = "Signup failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        signupError.setText(message);
                        signupError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void saveUserDocument(String uid, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);

        db.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // No-op
                })
                .addOnFailureListener(e -> {
                    // Don't block navigation
                });
    }

    private void goToRoleHome(String role) {
        Intent intent;

        if ("parent".equals(role)) {
            intent = new Intent(SignupActivity.this, ParentHomeActivity.class);
        } else if ("provider".equals(role)) {
            intent = new Intent(SignupActivity.this, ProviderHomeActivity.class);
        } else {
            intent = new Intent(SignupActivity.this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }
}