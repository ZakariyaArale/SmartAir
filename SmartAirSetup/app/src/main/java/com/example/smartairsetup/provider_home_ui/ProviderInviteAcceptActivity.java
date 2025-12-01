package com.example.smartairsetup.provider_home_ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class ProviderInviteAcceptActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText editToken;
    private TextView error;
    private Button buttonAccept;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_invite_accept);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editToken = findViewById(R.id.editToken);
        error = findViewById(R.id.error);
        buttonAccept = findViewById(R.id.buttonAccept);
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // If launched via smartair://invite?token=XYZ
        Uri data = getIntent().getData();
        if (data != null) {
            String token = data.getQueryParameter("token");
            if (!TextUtils.isEmpty(token)) {
                editToken.setText(token);
            }
        }

        buttonAccept.setOnClickListener(v -> accept());
    }

    private void accept() {
        error.setVisibility(View.GONE);
        error.setText("");

        if (mAuth.getCurrentUser() == null) {
            showError("You must be logged in as a provider.");
            return;
        }

        final String providerUid = mAuth.getCurrentUser().getUid();
        final String token = editToken.getText().toString().trim();

        if (TextUtils.isEmpty(token)) {
            showError("Paste the invite token.");
            return;
        }

        buttonAccept.setEnabled(false);

        DocumentReference inviteRef = db.collection("invites").document(token);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            var snap = transaction.get(inviteRef);
            if (!snap.exists()) throw new IllegalStateException("Invite not found.");

            Boolean revoked = snap.getBoolean("revoked");
            Timestamp expiresAt = snap.getTimestamp("expiresAt");
            String acceptedBy = snap.getString("acceptedBy");

            if (Boolean.TRUE.equals(revoked)) throw new IllegalStateException("Invite revoked.");
            if (expiresAt == null || expiresAt.compareTo(Timestamp.now()) <= 0)
                throw new IllegalStateException("Invite expired.");
            if (!TextUtils.isEmpty(acceptedBy))
                throw new IllegalStateException("Invite already used.");

            String parentUid = snap.getString("parentUid");
            String childId = snap.getString("childId");
            String childName = snap.getString("childName");

            if (TextUtils.isEmpty(parentUid) || TextUtils.isEmpty(childId))
                throw new IllegalStateException("Invite is invalid.");

            // mark invite used
            transaction.update(inviteRef,
                    "acceptedBy", providerUid,
                    "acceptedAt", FieldValue.serverTimestamp()
            );

            // create provider -> linkedChildren link
            String linkId = parentUid + "__" + childId;
            DocumentReference providerLink =
                    db.collection("users").document(providerUid)
                            .collection("linkedChildren").document(linkId);

            Map<String, Object> linkData = new HashMap<>();
            linkData.put("parentUid", parentUid);
            linkData.put("childId", childId);
            linkData.put("childName", childName == null ? "" : childName);
            linkData.put("linkedAt", FieldValue.serverTimestamp());

            transaction.set(providerLink, linkData);

            // track under child as well
            DocumentReference childProviderLink =
                    db.collection("users").document(parentUid)
                            .collection("children").document(childId)
                            .collection("providers").document(providerUid);

            Map<String, Object> childLink = new HashMap<>();
            childLink.put("providerUid", providerUid);
            childLink.put("linkedAt", FieldValue.serverTimestamp());

            transaction.set(childProviderLink, childLink);

            return null;
        }).addOnSuccessListener(unused -> {
            buttonAccept.setEnabled(true);
            finish(); // you can navigate to ProviderHomeActivity if you want
        }).addOnFailureListener(e -> {
            buttonAccept.setEnabled(true);
            showError(e.getMessage() == null ? "Failed to accept invite." : e.getMessage());
        });
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }
}