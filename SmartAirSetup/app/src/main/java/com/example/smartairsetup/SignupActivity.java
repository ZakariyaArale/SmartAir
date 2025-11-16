package com.example.smartairsetup;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText signupEmail;
    private EditText signupPassword;
    private RadioGroup radioGroupRole;
    private Button buttonSignup;
    private TextView signupError;

    // private FirebaseAuth mAuth; TODO: restore once google-services.json is available
    // private FirebaseFirestore db; TODO: restore once google-services.json is available

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // mAuth = FirebaseAuth.getInstance(); TODO: restore once google-services.json is available
        // db = FirebaseFirestore.getInstance(); TODO: restore once google-services.json is available

        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        buttonSignup = findViewById(R.id.buttonSignup);
        signupError = findViewById(R.id.signupError);

        buttonSignup.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        // Clear previous error
        signupError.setVisibility(TextView.GONE);
        signupError.setText("");

        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString();
        String role = getSelectedRole();

        // ---- Client-side validation ----
        // Email: not empty
        if (TextUtils.isEmpty(email)) {
            signupEmail.setError("Email is required");
            signupEmail.requestFocus();
            return;
        }

        // Email: must look like an email address
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Enter a valid email address");
            signupEmail.requestFocus();
            return;
        }

        // Password: not empty
        if (TextUtils.isEmpty(password)) {
            signupPassword.setError("Password is required");
            signupPassword.requestFocus();
            return;
        }

        // Password: at least 6 chars (Firebase’s minimum)
        if (password.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            signupPassword.requestFocus();
            return;
        }

        // Note: Firebase itself also enforces min length, and will throw a "weak password" error
        // if you somehow bypass this client-side check. We'll surface that below as well.

        // Role: must be selected
        if (role == null) {
            signupError.setText("Please select whether you are a Child, Parent, or Provider");
            signupError.setVisibility(TextView.VISIBLE);
            return;
        }

        // If we got here, input looks valid – prevent double clicks while we talk to Firebase.
        buttonSignup.setEnabled(false);
        /* TODO: restore Firebase sign-up once google-services.json is available
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    buttonSignup.setEnabled(true);

                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserRole(uid, role);
                    } else {
                        String message = "Signup failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        signupError.setText(message);
                        signupError.setVisibility(TextView.VISIBLE);
                    }
                });*/
        // Remove these two lines after google-services.json is available
        goToRoleHome(role);
        buttonSignup.setEnabled(true);
    }

    private String getSelectedRole() {
        int checkedId = radioGroupRole.getCheckedRadioButtonId();
        if (checkedId == R.id.radioChild) return "child";
        if (checkedId == R.id.radioParent) return "parent";
        if (checkedId == R.id.radioProvider) return "provider";
        return null;
    }
    /* TODO: restore once google-services.json is available
    private void saveUserRole(String uid, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", role);

        db.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    // Once created & role saved, send them to the auth flow
                    goToRoleHome(role);
                })
                .addOnFailureListener(e -> {
                    signupError.setText("Failed to save role: " + e.getMessage());
                    signupError.setVisibility(TextView.VISIBLE);
                });
    }
    */

    private void goToRoleHome(String role) {
        // TODO: replace these with real Activities for when Rohat creates them
        Intent intent = new Intent(this, LoginActivity.class);
        /*
        switch (role) {
            case "child":
                intent = new Intent(this, ChildHomeActivity.class);
                break;
            case "parent":
                intent = new Intent(this, ParentHomeActivity.class);
                break;
            case "provider":
                intent = new Intent(this, ProviderHomeActivity.class);
                break;
            default:
                intent = new Intent(this, LoginActivity.class);
        }*/
        startActivity(intent);
        finish();
    }
}
