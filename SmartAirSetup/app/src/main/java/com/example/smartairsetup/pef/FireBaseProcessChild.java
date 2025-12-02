package com.example.smartairsetup.pef;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FireBaseProcessChild implements ProcessChildren {

    private final String parentID;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FireBaseProcessChild() {
        parentID = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
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

                    // Store all child Ids here
                    List<UserID> childrenList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        String uid = doc.getId();

                        if (name == null) {
                            continue;
                        }

                        childrenList.add(new UserID(uid, name));
                    }

                    listener.onChildrenLoaded(childrenList);
                })
                .addOnFailureListener(listener::onError);
    }
}
