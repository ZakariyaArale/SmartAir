package com.example.smartairsetup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireBaseProcessMedication implements ProcessChildren {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String parentID;
    private final String childID;

    public FireBaseProcessMedication(String childID) {
        this.childID = childID;
        this.parentID = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @Override
    public void getChildren(ChildFetchListener listener) {
        getMedications(listener);
    }

    private void getMedications(ChildFetchListener listener) {
        if (parentID == null || childID == null) {
            listener.onError(new Exception("Parent or child not set"));
            return;
        }

        db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childID)
                .collection("medications")
                .get()
                .addOnSuccessListener(query -> {
                    List<UserID> medList = new ArrayList<>();
                    for (var doc : query) {
                        String medName = doc.getString("name");
                        String medUUID = doc.getString("med_UUID");
                        if (medName == null || medUUID == null) continue;
                        medList.add(new UserID(medUUID, medName));
                    }
                    listener.onChildrenLoaded(medList);
                })
                .addOnFailureListener(listener::onError);
    }

    public void recordMedicationUsage(String medUUID, String medName, int dosage, MedicationCallback callback) {
        if (parentID == null || childID == null || medUUID == null || medName == null) {
            callback.onComplete(false);
            return;
        }

        DocumentReference medRef = db.collection("users")
                .document(parentID)
                .collection("children")
                .document(childID)
                .collection("medications")
                .document(medUUID);

        // Step 1: Update puffsLeft
        medRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                callback.onComplete(false);
                return;
            }

            Long currentPuffs = doc.getLong("puffsLeft");
            if (currentPuffs == null) currentPuffs = 0L;

            long updatedPuffs = currentPuffs - dosage;
            if (updatedPuffs < 0) updatedPuffs = 0;

            medRef.update("puffsLeft", updatedPuffs)
                    .addOnSuccessListener(aVoid -> {
                        // Step 2: Add entry to the triage log
                        String dateId = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                        DocumentReference logRef = db.collection("users")
                                .document(parentID)
                                .collection("children")
                                .document(childID)
                                .collection("triage")
                                .document("logs")
                                .collection("entries")
                                .document(dateId);

                        logRef.get().addOnSuccessListener(docSnap -> {
                            List<Map<String, Object>> medLogList;

                            if (docSnap.exists() && docSnap.contains("medicationLog")) {
                                medLogList = (List<Map<String, Object>>) docSnap.get("medicationLog");
                            } else {
                                medLogList = new ArrayList<>();
                            }

                            // Add new entry
                            Map<String, Object> newEntry = new HashMap<>();
                            newEntry.put("name", medName);
                            newEntry.put("dosage", dosage);
                            newEntry.put("timestamp", System.currentTimeMillis());

                            medLogList.add(newEntry);

                            Map<String, Object> updateMap = new HashMap<>();
                            updateMap.put("medicationLog", medLogList);

                            logRef.set(updateMap, SetOptions.merge())
                                    .addOnSuccessListener(aVoid1 -> callback.onComplete(true))
                                    .addOnFailureListener(e -> callback.onComplete(false));
                        }).addOnFailureListener(e -> callback.onComplete(false));
                    })
                    .addOnFailureListener(e -> callback.onComplete(false));

        }).addOnFailureListener(e -> callback.onComplete(false));
    }

    public interface MedicationCallback {
        void onComplete(boolean success);
    }
}