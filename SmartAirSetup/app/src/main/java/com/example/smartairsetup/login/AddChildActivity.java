package com.example.smartairsetup.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddChildActivity extends AppCompatActivity {

    private static final int DEFAULT_PB = 0;
    private EditText editChildUsername;
    private EditText editChildPassword;
    private EditText editChildConfirmPassword;
    private EditText editChildName;
    private EditText editChildDob;
    private EditText editChildNotes;
    private TextView textChildError;
    private Button buttonSaveChild;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
        textChildError.setText(null);

        String username = editChildUsername.getText().toString().trim();
        String password = editChildPassword.getText().toString();
        String confirmPassword = editChildConfirmPassword.getText().toString();
        String name = editChildName.getText().toString().trim();
        String dob = editChildDob.getText().toString().trim();
        String notes = editChildNotes.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editChildUsername.setError(getString(R.string.error_username_required));
            editChildUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editChildPassword.setError(getString(R.string.error_password_required));
            editChildPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editChildPassword.setError(getString(R.string.error_password_length));
            editChildPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editChildConfirmPassword.setError(getString(R.string.error_password_mismatch));
            editChildConfirmPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(name)) {
            editChildName.setError(getString(R.string.error_child_name_required));
            editChildName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(dob)) {
            editChildDob.setError(getString(R.string.error_dob_required));
            editChildDob.requestFocus();
            return;
        }

        String parentUid = (mAuth.getCurrentUser() != null)
                ? mAuth.getCurrentUser().getUid()
                : null;

        if (parentUid == null) {
            textChildError.setText(R.string.error_must_be_logged_in_parent);
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
                        textChildError.setText(R.string.error_child_username_taken);
                        textChildError.setVisibility(View.VISIBLE);
                        buttonSaveChild.setEnabled(true);
                    } else {
                        // Safe to create a new child
                        createChild(parentUid, username, password, name, dob, notes);
                    }
                })
                .addOnFailureListener(e -> {
                    textChildError.setText(
                            getString(R.string.error_failed_check_username, e.getMessage())
                    );
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

        childData.put("shareRescueLogs", false);
        childData.put("shareControllerSummary", false);
        childData.put("shareSymptoms", false);
        childData.put("shareTriggers", false);
        childData.put("sharePEF", false);
        childData.put("shareTriageIncidents", false);
        childData.put("shareSummaryCharts", false);

        childrenRef.add(childData)
                .addOnSuccessListener(childDocRef -> {
                    String childDocId = childDocRef.getId();

                    Map<String, Object> accountData = new HashMap<>();
                    accountData.put("username", username);
                    accountData.put("password", password);
                    accountData.put("parentUid", parentUid);
                    accountData.put("childDocId", childDocId);
                    accountData.put("firstTime", true);

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
                                textChildError.setText(
                                        getString(R.string.error_failed_save_child_account, e.getMessage())
                                );
                                textChildError.setVisibility(View.VISIBLE);
                                buttonSaveChild.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    textChildError.setText(
                            getString(R.string.error_failed_add_child, e.getMessage())
                    );
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }
}