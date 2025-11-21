package com.example.smartairsetup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddChildActivity extends AppCompatActivity {

    private static final int DEFAULT_PB = 0;
    private static final int DEFAULT_PEF = 0;
    private static final int DEFAULT_PMED = 0;

    private EditText editChildEmail;
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

        editChildEmail = findViewById(R.id.editChildEmail);
        editChildName = findViewById(R.id.editChildName);
        editChildDob = findViewById(R.id.editChildDob);
        editChildNotes = findViewById(R.id.editChildNotes);
        textChildError = findViewById(R.id.textChildError);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        buttonSaveChild.setOnClickListener(v -> saveChild());
    }

    private void saveChild() {
        textChildError.setVisibility(View.GONE);
        textChildError.setText("");

        String childEmail = editChildEmail.getText().toString().trim();
        String name = editChildName.getText().toString().trim();
        String dob = editChildDob.getText().toString().trim();
        String notes = editChildNotes.getText().toString().trim();

        // Basic validation for fields that are ALWAYS required
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

        // Email is OPTIONAL now:
        // - If provided → must be valid format and must match an existing child account
        // - If empty → we'll create a "local" child profile with no login
        boolean hasEmail = !TextUtils.isEmpty(childEmail);
        if (hasEmail && !Patterns.EMAIL_ADDRESS.matcher(childEmail).matches()) {
            editChildEmail.setError("Enter a valid email address");
            editChildEmail.requestFocus();
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

        if (hasEmail) {
            // 🔹 OLDER CHILD FLOW: child has their own account
            linkExistingChildAccount(parentUid, childEmail, name, dob, notes);
        } else {
            // 🔹 YOUNGER CHILD FLOW: no email / no account
            createLocalChildProfile(parentUid, name, dob, notes);
        }
    }

    /**
     * Flow 1: child already has an account (email provided).
     * We look up the child in `users` by email + role='child' and link using their uid.
     */
    private void linkExistingChildAccount(@NonNull String parentUid,
                                          @NonNull String childEmail,
                                          @NonNull String name,
                                          @NonNull String dob,
                                          @NonNull String notes) {

        db.collection("users")
                .whereEqualTo("email", childEmail)
                .whereEqualTo("role", "child")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        textChildError.setText("No child account found with this email.");
                        textChildError.setVisibility(View.VISIBLE);
                        buttonSaveChild.setEnabled(true);
                        return;
                    }

                    DocumentSnapshot childDoc = querySnapshot.getDocuments().get(0);
                    String childUid = childDoc.getId();

                    linkChildToParent(parentUid, childUid, childEmail, name, dob, notes);
                })
                .addOnFailureListener(e -> {
                    textChildError.setText("Failed to look up child: " + e.getMessage());
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }

    /**
     * Flow 2: child does NOT have an account (no email).
     * We just create a child profile under the parent with an auto-generated ID.
     */
    private void createLocalChildProfile(@NonNull String parentUid,
                                         @NonNull String name,
                                         @NonNull String dob,
                                         @NonNull String notes) {

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        Map<String, Object> childData = new HashMap<>();
        childData.put("childUid", null);          // no linked auth account
        childData.put("email", null);             // no email
        childData.put("name", name);
        childData.put("dateOfBirth", dob);
        if (!TextUtils.isEmpty(notes)) {
            childData.put("notes", notes);
        }
        childData.put("pb", DEFAULT_PB);
        childData.put("pef", DEFAULT_PEF);
        childData.put("pre-med", DEFAULT_PMED);
        childData.put("post-med", DEFAULT_PMED);

        // Auto-generate a document ID for this local child
        childrenRef.add(childData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(AddChildActivity.this,
                            "Child added successfully",
                            Toast.LENGTH_SHORT).show();
                    buttonSaveChild.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    textChildError.setText("Failed to add child: " + e.getMessage());
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }

    /**
     * Shared logic for linking to an existing child user (has account).
     * Uses childUid as the document ID so the same child can't be linked twice.
     */
    private void linkChildToParent(@NonNull String parentUid,
                                   @NonNull String childUid,
                                   @NonNull String childEmail,
                                   @NonNull String name,
                                   @NonNull String dob,
                                   @NonNull String notes) {

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        Map<String, Object> childData = new HashMap<>();
        childData.put("childUid", childUid);
        childData.put("email", childEmail);
        childData.put("name", name);
        childData.put("dateOfBirth", dob);
        if (!TextUtils.isEmpty(notes)) {
            childData.put("notes", notes);
        }
        childData.put("pb", DEFAULT_PB);
        childData.put("pef", DEFAULT_PEF);
        childData.put("pre-med", DEFAULT_PMED);
        childData.put("post-med", DEFAULT_PMED);

        // Use childUid as the document ID so you can't link the same child twice for this parent
        childrenRef.document(childUid)
                .set(childData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AddChildActivity.this,
                            "Child linked successfully",
                            Toast.LENGTH_SHORT).show();
                    buttonSaveChild.setEnabled(true);
                    finish(); // go back to parent screen
                })
                .addOnFailureListener(e -> {
                    textChildError.setText("Failed to link child: " + e.getMessage());
                    textChildError.setVisibility(View.VISIBLE);
                    buttonSaveChild.setEnabled(true);
                });
    }
}