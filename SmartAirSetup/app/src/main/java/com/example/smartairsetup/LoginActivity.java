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

    private EditText editTextIdentifier; // email (parent/provider) OR child username
    private EditText editTextPassword;
    private Button buttonSignIn;
    private TextView textViewError;
    private TextView textViewForgotPassword;
    private Button backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        editTextIdentifier = findViewById(R.id.editTextIdentifier);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        textViewError = findViewById(R.id.textViewError);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonSignIn.setOnClickListener(v -> handleSignIn());
        textViewForgotPassword.setOnClickListener(v -> handleForgotPassword());
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void handleSignIn() {
        textViewError.setVisibility(View.GONE);
        textViewError.setText("");

        String identifier = editTextIdentifier.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (TextUtils.isEmpty(identifier)) {
            editTextIdentifier.setError("Email or username is required");
            editTextIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        buttonSignIn.setEnabled(false);

        if (identifier.contains("@")) {
            // Parent/provider login via Firebase Auth (email)
            signInParentOrProvider(identifier, password);
        } else {
            // Child login via Firestore childAccounts
            signInChild(identifier, password);
        }
    }

    private void signInParentOrProvider(String email, String password) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextIdentifier.setError("Enter a valid email");
            editTextIdentifier.requestFocus();
            buttonSignIn.setEnabled(true);
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        fetchUserRoleAndNavigate(uid);
                    } else {
                        buttonSignIn.setEnabled(true);
                        String message = "Sign in failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        textViewError.setText(message);
                        textViewError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void signInChild(String username, String password) {
        db.collection("childAccounts")
                .document(username)
                .get()
                .addOnSuccessListener(doc -> {
                    buttonSignIn.setEnabled(true);

                    if (!doc.exists()) {
                        textViewError.setText("No child account found with this username.");
                        textViewError.setVisibility(View.VISIBLE);
                        return;
                    }

                    String storedPassword = doc.getString("password");
                    if (storedPassword == null || !storedPassword.equals(password)) {
                        textViewError.setText("Incorrect password for this child account.");
                        textViewError.setVisibility(View.VISIBLE);
                        return;
                    }

                    String parentUid = doc.getString("parentUid");
                    String childDocId = doc.getString("childDocId");

                    // Navigate to child home screen (parent for now since no child)
                    Intent intent = new Intent(LoginActivity.this, ParentHomeActivity.class);
                    intent.putExtra("PARENT_UID", parentUid);
                    intent.putExtra("CHILD_DOC_ID", childDocId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    buttonSignIn.setEnabled(true);
                    textViewError.setText("Failed to sign in child: " + e.getMessage());
                    textViewError.setVisibility(View.VISIBLE);
                });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    buttonSignIn.setEnabled(true);

                    if (document != null && document.exists()) {
                        String role = document.getString("role");
                        goToRoleHome(role);
                    } else {
                        goToRoleHome(null);
                    }
                })
                .addOnFailureListener(e -> {
                    buttonSignIn.setEnabled(true);
                    textViewError.setText("Failed to load user role: " + e.getMessage());
                    textViewError.setVisibility(View.VISIBLE);
                });
    }

    private void handleForgotPassword() {
        textViewError.setVisibility(View.GONE);
        String identifier = editTextIdentifier.getText().toString().trim();

        if (TextUtils.isEmpty(identifier)) {
            editTextIdentifier.setError("Enter your email to reset password");
            editTextIdentifier.requestFocus();
            return;
        }

        if (!identifier.contains("@")) {
            // It's a child username; no Firebase reset
            textViewError.setText("Password reset is only available for parent/provider emails.");
            textViewError.setVisibility(View.VISIBLE);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            editTextIdentifier.setError("Enter a valid email");
            editTextIdentifier.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(identifier)
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
        Intent intent;

        if ("parent".equals(role)) {
            intent = new Intent(this, ParentHomeActivity.class);
        } else if ("provider".equals(role)) {
            // for now just redirect to parent home screen
            intent = new Intent(this, ParentHomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }
}