package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonSignIn;
    private TextView textViewError;
    private TextView textViewForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        textViewError = findViewById(R.id.textViewError);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonSignIn.setOnClickListener(v -> handleSignIn());
        textViewForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleSignIn() {
        textViewError.setVisibility(View.GONE);
        textViewError.setText("");

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        buttonSignIn.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonSignIn.setEnabled(true);

                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        fetchUserRoleAndNavigate(uid);
                    } else {
                        String message = "Sign in failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        textViewError.setText(message);
                        textViewError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        String role = document.getString("role");
                        goToRoleHome(role);
                    } else {
                        goToRoleHome(null);
                    }
                })
                .addOnFailureListener(e -> {
                    textViewError.setText("Failed to load user role: " + e.getMessage());
                    textViewError.setVisibility(View.VISIBLE);
                });
    }

    private void handleForgotPassword() {
        textViewError.setVisibility(View.GONE);
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Enter your email to reset password");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        String message = "Failed to send reset email";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        textViewError.setText(message);
                        textViewError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void goToRoleHome(String role) {
        // TODO: replace with real Activities when your team adds them
        Intent intent = new Intent(this, MainActivity.class);

        if ("child".equals(role)) {
            intent = new Intent(this, ChildHomeActivity.class);
        } else if ("parent".equals(role)) {
            intent = new Intent(this, ParentHomeActivity.class);
        } else if ("provider".equals(role)) {
            intent = new Intent(this, ProviderHomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
