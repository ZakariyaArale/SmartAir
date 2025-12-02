package com.example.smartairsetup.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.smartairsetup.R;
import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.login.SignupActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OnboardingActivity4 extends AbstractOnboarding {

    private String parentUid;
    private String childId;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_onboarding4;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve child onboarding data if present
        parentUid = getIntent().getStringExtra("PARENT_UID");
        childId = getIntent().getStringExtra("CHILD_ID");

        setNextButton();
        setBackButton();
    }

    private void setNextButton() {
        Button nextButton = findViewById(R.id.nextButton);
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> {
                if (parentUid != null && childId != null) {
                    CollectionReference childAccountsRef = db.collection("childAccounts");

                    childAccountsRef
                            .whereEqualTo("childDocId", childId)
                            .whereEqualTo("parentUid", parentUid)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    for (QueryDocumentSnapshot doc : querySnapshot) {
                                        Boolean firstTime = doc.getBoolean("firstTime");
                                        if (firstTime != null && firstTime) {
                                            // Update firstTime to false
                                            doc.getReference()
                                                    .update("firstTime", false)
                                                    .addOnSuccessListener(aVoid ->
                                                            Log.d("Onboarding4", "firstTime set to false"))
                                                    .addOnFailureListener(e ->
                                                            Log.e("Onboarding4", "Failed to update firstTime", e));
                                        }
                                    }
                                } else {
                                    Log.e("Onboarding4", "No matching child found in childAccounts");
                                }

                                // Navigate to ChildHomeActivity (do NOT send firstTime)
                                Intent intent = new Intent(this, ChildHomeActivity.class);
                                intent.putExtra("PARENT_UID", parentUid);
                                intent.putExtra("CHILD_ID", childId);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Onboarding4", "Failed to query childAccounts", e);
                                Toast.makeText(this, "Error accessing child account.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Fallback if no child/parent info
                    Intent intent = new Intent(this, SignupActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void setBackButton() {
        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, OnboardingActivity3.class);
                if (parentUid != null) intent.putExtra("PARENT_UID", parentUid);
                if (childId != null) intent.putExtra("CHILD_ID", childId);
                startActivity(intent);
                finish();
            });
        }
    }
}
