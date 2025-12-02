package com.example.smartairsetup.provider_home_ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.example.smartairsetup.login.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class ProviderHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.smartairsetup.R.layout.activity_provider_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        container = findViewById(com.example.smartairsetup.R.id.providerChildrenContainer);

        Button buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent signOutIntent = new Intent(this, MainActivity.class);
            // Make sure the user cannot go back to the main screen after signing out
            signOutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signOutIntent);
            finish();
        });

        loadSharedChildren();
    }

    private void loadSharedChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in as provider.", Toast.LENGTH_SHORT).show();
            return;
        }

        String providerUid = mAuth.getCurrentUser().getUid();

        db.collectionGroup("children")
                .whereArrayContains("sharedProviderUids", providerUid)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Failed to load: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;

                    container.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(this);

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String childName = doc.getString("name");
                        String childId = doc.getId();

                        String parentUid =
                                doc.getReference().getParent().getParent().getId();

                        View row = inflater.inflate(R.layout.item_provider_child, container, false);
                        TextView nameTv = row.findViewById(R.id.textChildNameRow);
                        Button viewBtn = row.findViewById(R.id.buttonViewChild);

                        nameTv.setText(childName != null ? childName : "(Unnamed child)");

                        viewBtn.setOnClickListener(v -> {
                            Intent i = new Intent(this, ProviderChildPortalActivity.class);
                            i.putExtra(ProviderChildPortalActivity.EXTRA_PARENT_UID, parentUid);
                            i.putExtra(ProviderChildPortalActivity.EXTRA_CHILD_ID, childId);
                            i.putExtra(ProviderChildPortalActivity.EXTRA_CHILD_NAME, childName);
                            startActivity(i);
                        });

                        container.addView(row);
                    }
                });
    }
}