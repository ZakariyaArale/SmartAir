package com.example.smartairsetup.notification;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AlertRepository {

    private final FirebaseFirestore db;

    public AlertRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendAlert(
            String parentUid,
            String childId,
            String type,
            String message,
            @Nullable OnSuccessListener<Void> onSuccess,
            @Nullable OnFailureListener onFailure
    ) {
        if (parentUid == null || parentUid.isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("parentUid is null/empty"));
            }
            return;
        }

        if (childId == null || childId.isEmpty()) {
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("childId is null/empty"));
            }
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("parentUid", parentUid);
        data.put("childId", childId);
        data.put("type", type);
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("handled", false);

        db.collection("users")
                .document(parentUid)
                .collection("alerts")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    if (onSuccess != null) onSuccess.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }
}