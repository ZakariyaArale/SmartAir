package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class ProviderHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        container = findViewById(R.id.providerChildrenContainer);

        loadSharedChildren();
    }

    private void loadSharedChildren() {
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

                        // users/{parentUid}/children/{childId}
                        String parentUid = doc.getReference().getParent().getParent().getId();

                        android.view.View row = inflater.inflate(R.layout.item_provider_child, container, false);

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