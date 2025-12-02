package com.example.smartairsetup.notification;

import com.google.firebase.firestore.FirebaseFirestore;

public class RapidRescueCountHelper {

    public static void checkRescueRepeats(
            String parentUid,
            String childId,
            RescueCheckCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        long now = System.currentTimeMillis();
        long threeHoursAgo = now - (3 * 60 * 60 * 1000); //timestamp is in milliseconds

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medLogs")
                .whereEqualTo("isRescue", true)
                .whereGreaterThanOrEqualTo("timestamp", threeHoursAgo)
                .get()
                .addOnSuccessListener(recentRescueLogsSnap -> {

                    int rescueCount = recentRescueLogsSnap.size();

                    boolean moreThanTwo = rescueCount >= 3;

                    callback.onResult(moreThanTwo);
                })
                .addOnFailureListener(e -> {
                    // Fail safe: treat as NOT triggered
                    callback.onResult(false);
                });
    }


}
