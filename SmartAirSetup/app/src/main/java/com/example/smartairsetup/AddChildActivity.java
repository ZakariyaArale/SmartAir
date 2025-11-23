package com.example.smartairsetup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class AddChildActivity extends AppCompatActivity {

    private static final int DEFAULT_PB = 0;
    private static final int DEFAULT_PEF = 0;
    private static final int DEFAULT_PMED = 0;

    private EditText editChildUsername;
    private EditText editChildPassword;
    private EditText editChildConfirmPassword;
    private EditText editChildName;
    private EditText editChildDob;
    private EditText editChildNotes;
    private TextView textChildError;
    private Button buttonSaveChild;
    private Button backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        editChildUsername = findViewById(R.id.editChildUsername);
        editChildPassword = findViewById(R.id.editChildPassword);
        editChildConfirmPassword = findViewById(R.id.editChildConfirmPassword);
        editChildName = findViewById(R.id.editChildName);
        editChildDob = findViewById(R.id.editChildDob);
        editChildNotes = findViewById(R.id.editChildNotes);
        textChildError = findViewById(R.id.textChildError);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        buttonSaveChild.setOnClickListener(v -> saveChild());
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void saveChild() {
        textChildError.setVisibility(View.GONE);
        textChildError.setText("");

        String username = editChildUsername.getText().toString().trim();
        String password = editChildPassword.getText().toString();
        String confirmPassword = editChildConfirmPassword.getText().toString();
        String name = editChildName.getText().toString().trim();
        String dob = editChildDob.getText().toString().trim();
        String notes = editChildNotes.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editChildUsername.setError("Username is required");
            editChildUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editChildPassword.setError("Password is required");
            editChildPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editChildPassword.setError("Password must be at least 6 characters");
            editChildPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editChildConfirmPassword.setError("Passwords do not match");
            editChildConfirmPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            editChildName.setError("Child name is required");
            editChildName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(dob)) {
            editChildDob.setError("Date of birth is required");
            editChildDob.requestFocus();
            return;
        }

        String parentUid = (mAuth.getCurrentUser() != null)
                ? mAuth.getCurrentUser().getUid()
                : null;

        if (parentUid == null) {
            textChildError.setText("You must be logged in as a parent.");
            textChildError.setVisibility(View.VISIBLE);
            return;
        }

        buttonSaveChild.setEnabled(false);

        // Check if username already exists in childAccounts
        db.collection("childAccounts")
                .document(username)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        textChildError.setText("This username is already taken. Choose another one.");
                        textChildError.setVisibility(View.VISIBLE);
                        buttonSaveChild.setEnabled(true);
                    } else {
                        // Safe to create a new child
                        createChild(parentUid, username, password, name, dob, notes);
                    }
                })
                .addOnFailureListener(e -> {
                    textChildError.setText("Failed to check username: " + e.getMessage());
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }

    private void createChild(@NonNull String parentUid,
                             @NonNull String username,
                             @NonNull String password,
                             @NonNull String name,
                             @NonNull String dob,
                             @NonNull String notes) {

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        Map<String, Object> childData = new HashMap<>();
        childData.put("username", username);
        childData.put("name", name);
        childData.put("dateOfBirth", dob);
        if (!TextUtils.isEmpty(notes)) {
            childData.put("notes", notes);
        }
        childData.put("pb", DEFAULT_PB);
        childData.put("pef", DEFAULT_PEF);
        childData.put("pre-med", DEFAULT_PMED);
        childData.put("post-med", DEFAULT_PMED);
        childData.put("rescue-triage", DEFAULT_PMED);
        childData.put("pef-triage", DEFAULT_PMED);
        childData.put("message-triage", "None");



        // Sharing options default
        childData.put("shareRescueLogs", false);
        childData.put("shareControllerSummary", false);
        childData.put("shareSymptoms", false);
        childData.put("shareTriggers", false);
        childData.put("sharePEF", false);
        childData.put("shareTriageIncidents", false);
        childData.put("shareSummaryCharts", false);

        // First create the child under the parent so we get the childDocId
        childrenRef.add(childData)
                .addOnSuccessListener(childDocRef -> {
                    String childDocId = childDocRef.getId();

                    // Now create top-level childAccounts/{username}
                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("username", username);
                    accountData.put("password", password); // Plaintext for now (hackathon level)
                    accountData.put("parentUid", parentUid);
                    accountData.put("childDocId", childDocId);

                    db.collection("childAccounts")
                            .document(username)
                            .set(accountData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(AddChildActivity.this,
                                        "Child added successfully",
                                        Toast.LENGTH_SHORT).show();
                                buttonSaveChild.setEnabled(true);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                textChildError.setText("Failed to save child account: " + e.getMessage());
                                textChildError.setVisibility(View.VISIBLE);
                                buttonSaveChild.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    textChildError.setText("Failed to add child: " + e.getMessage());
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }
}