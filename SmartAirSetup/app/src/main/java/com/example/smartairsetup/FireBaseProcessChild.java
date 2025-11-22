package com.example.smartairsetup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FireBaseProcessChild implements ProcessChildren {

    private final String parentID;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FireBaseProcessChild() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            parentID = auth.getCurrentUser().getUid();
        } else {
            parentID = null; // optional: you could throw or log here
        }
    }

    @Override
    public void getChildren(ChildFetchListener listener) {

        if (parentID == null || parentID.isEmpty()) {
            listener.onError(new Exception("No parent logged in"));
            return;
        }

        db.collection("users")
                .document(parentID)
                .collection("children")
                .get()
                .addOnSuccessListener(query -> {

                    // Store all child IDs here
                    List<UserID> childrenList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) {
                            continue;
                        }

                        // Try childUid field first (linked child)
                        String uid = doc.getString("childUid");

                        // If there is no childUid (local child), use the document ID
                        if (uid == null || uid.trim().isEmpty()) {
                            uid = doc.getId();
                        }

                        childrenList.add(new UserID(uid, name));
                    }

                    listener.onChildrenLoaded(childrenList);
                })
                .addOnFailureListener(listener::onError);
    }
}