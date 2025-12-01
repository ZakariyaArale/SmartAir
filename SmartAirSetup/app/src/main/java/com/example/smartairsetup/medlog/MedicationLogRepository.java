package com.example.smartairsetup.medlog;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MedicationLogRepository {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public MedicationLogRepository() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // --- CONTROLLER LOGS ---

    public void logControllerDose(
            String childId,
            int doseCount,
            @Nullable OnSuccessListener<Void> onSuccess,
            @Nullable OnFailureListener onFailure
    ) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || childId == null) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("Not signed in or no childId"));
            }
            return;
        }

        String parentUid = user.getUid();
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(now));

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", now);
        data.put("date", dateStr);
        data.put("doseCount", doseCount);
        data.put("source", "parent");

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationLogs_controller")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    if (onSuccess != null) onSuccess.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    // --- RESCUE LOGS (for later, Story 4) ---

    public void logRescueDose(
            String childId,
            int doseCount,
            @Nullable OnSuccessListener<Void> onSuccess,
            @Nullable OnFailureListener onFailure
    ) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || childId == null) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("Not signed in or no childId"));
            }
            return;
        }

        String parentUid = user.getUid();
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(now));

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", now);
        data.put("date", dateStr);
        data.put("doseCount", doseCount);
        data.put("source", "parent");

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medicationLogs_rescue")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    if (onSuccess != null) onSuccess.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }
}