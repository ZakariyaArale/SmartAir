package com.example.smartairsetup;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FireBaseProcessMedication implements ProcessChildren {

    private static final String TAG = "FireBaseProcessMed";

    private final String parentUid;
    private final String childUid;
    private final FirebaseFirestore db;

    public FireBaseProcessMedication(String parentUid, String childUid) {
        this.parentUid = parentUid;
        this.childUid = childUid;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getChildren(ChildFetchListener listener) {
        Log.d(TAG, "Fetching medications for child UID: " + childUid + " under parent UID: " + parentUid);

        db.collection("users")
                .document(parentUid) // Use actual parent UID now
                .collection("children")
                .document(childUid)
                .collection("medications")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Fetched " + snapshot.size() + " medication docs for child " + childUid);
                    List<UserID> medsList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String name = doc.getString("name");
                        String uid = doc.getId();
                        Log.d(TAG, "Med doc: " + uid + ", name: " + name);
                        medsList.add(new UserID(uid, name));
                    }
                    listener.onChildrenLoaded(medsList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch medications for child " + childUid, e);
                    listener.onError(e);
                });
    }
}
