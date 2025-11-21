package com.example.smartairsetup;

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

        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        buttonSignup = findViewById(R.id.buttonSignup);
        signupError = findViewById(R.id.signupError);

        buttonSignup.setOnClickListener(v -> handleSignup());
    }

    private String getSelectedRole() {
        int checkedId = radioGroupRole.getCheckedRadioButtonId();
        if (checkedId == R.id.radioChild) return "child";
        if (checkedId == R.id.radioParent) return "parent";
        if (checkedId == R.id.radioProvider) return "provider";
        return null;
    }

    private void handleSignup() {
        // Clear previous error
        signupError.setVisibility(View.GONE);
        signupError.setText("");

        buttonSignup.setEnabled(false);

        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString();
        String role = getSelectedRole();

        // ---- Client-side validation ----

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
            signupError.setText("Please select whether you are a Child, Parent, or Provider");
            signupError.setVisibility(View.VISIBLE);
            buttonSignup.setEnabled(true);
            return;
        }

        // If we got here, input looks valid – go to Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // 🔥 FIRE & FORGET: save role but DON'T block navigation
                        saveUserDocument(uid, email, role);

                        Toast.makeText(SignupActivity.this,
                                "Account created!",
                                Toast.LENGTH_SHORT).show();

                        buttonSignup.setEnabled(true);
                        goToRoleHome(role);   // ✅ ALWAYS navigate after signup
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

        if ("child".equals(role)) {
            userData.put("name", "");              // default name
            userData.put("dateOfBirth", "");       // default DOB
            userData.put("notes", "");             // default notes
            userData.put("pb", 0);                 // default PB
            userData.put("pef", 0);                // default PEF
            userData.put("pre-med", 0);            // default pre-med
            userData.put("post-med", 0);           // default post-med
        }

        db.collection("users")
                .document(uid)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // optional: debug toast/log only
                    // Toast.makeText(this, "User doc saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // don't block navigation here, just surface the problem
                    // you already navigate right after Auth success
                    // so just show an error if you want
                    // signupError.setText("Failed to save user data: " + e.getMessage());
                    // signupError.setVisibility(View.VISIBLE);
                });
    }

    private void goToRoleHome(String role) {
        // For now: just go to LoginActivity so they can sign in
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
