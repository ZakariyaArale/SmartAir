package com.example.smartairsetup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FireBaseProcessChild implements ProcessChildren {

    private final String parentID;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FireBaseProcessChild() {
        // TODO: Replace with actual parent ID  FirebaseAuth.getInstance().getCurrentUser().getUid();
        parentID = "qGVzsSb3PMaI3D0UumcwJpuMgMG2";
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

                    //Store all child Ids here!
                    List<UserID> childrenList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("name");
                        String uid = doc.getString("childUid");

                        if (name == null || uid == null) {
                            continue;
                        }

                        childrenList.add(new UserID(uid, name));
                    }

                    listener.onChildrenLoaded(childrenList);
                })
                .addOnFailureListener(listener::onError);
    }
}