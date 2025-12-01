package com.example.smartairsetup.sharing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class InviteProviderActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "extra_child_id";
    public static final String EXTRA_CHILD_NAME = "extra_child_name";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView textInviteLink;
    private String parentUid;
    private String childId;
    private String childName;

    // keep last token so revoke/regenerate is easy
    private String currentToken = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_provider);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (mAuth.getCurrentUser() == null || childId == null) {
            Toast.makeText(this, "Missing parent/child", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        parentUid = mAuth.getCurrentUser().getUid();

        TextView textChild = findViewById(R.id.textChild);
        textChild.setText("Child: " + (childName == null ? "" : childName));

        textInviteLink = findViewById(R.id.textInviteLink);

        Button buttonGenerate = findViewById(R.id.buttonGenerate);
        Button buttonCopy = findViewById(R.id.buttonCopy);
        Button buttonShare = findViewById(R.id.buttonShare);
        Button buttonRevoke = findViewById(R.id.buttonRevoke);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        buttonGenerate.setOnClickListener(v -> regenerateInvite());
        buttonCopy.setOnClickListener(v -> copyLink());
        buttonShare.setOnClickListener(v -> shareLink());
        buttonRevoke.setOnClickListener(v -> revokeInvite());
    }

    private void regenerateInvite() {
        // revoke old token if we have one
        if (currentToken != null) {
            revokeTokenSilently(currentToken);
        }

        // Firestore auto-id works great as token
        String token = db.collection("invites").document().getId();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Timestamp expiresAt = new Timestamp(cal.getTime());

        Map<String, Object> data = new HashMap<>();
        data.put("parentUid", parentUid);
        data.put("childId", childId);
        data.put("childName", childName == null ? "" : childName);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("expiresAt", expiresAt);
        data.put("revoked", false);
        data.put("revokedAt", null);
        data.put("acceptedBy", null);
        data.put("acceptedAt", null);

        db.collection("invites")
                .document(token)
                .set(data)
                .addOnSuccessListener(unused -> {
                    currentToken = token;
                    textInviteLink.setText(buildInviteLink(token));
                    Toast.makeText(this, "Invite link generated (valid 7 days)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void revokeInvite() {
        if (currentToken == null) {
            Toast.makeText(this, "No active invite to revoke.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("invites")
                .document(currentToken)
                .update(
                        "revoked", true,
                        "revokedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Invite revoked.", Toast.LENGTH_SHORT).show();
                    textInviteLink.setText("Invite revoked.");
                    currentToken = null;
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void revokeTokenSilently(String token) {
        db.collection("invites").document(token)
                .update("revoked", true, "revokedAt", FieldValue.serverTimestamp());
    }

    private String buildInviteLink(String token) {
        return "smartair://invite?token=" + token;
    }

    private void copyLink() {
        String link = textInviteLink.getText().toString();
        if (!link.startsWith("smartair://")) {
            Toast.makeText(this, "Generate a link first.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Invite Link", link));
        Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show();
    }

    private void shareLink() {
        String link = textInviteLink.getText().toString();
        if (!link.startsWith("smartair://")) {
            Toast.makeText(this, "Generate a link first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "SmartAir provider invite (valid 7 days):\n" + link);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share invite link"));
    }
}