package com.example.smartairsetup;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ShareFlagsRepo {

    public interface Callback {
        void onFlags(@NonNull ShareFlags flags);
        void onError(@NonNull Exception e);
    }

    private ListenerRegistration reg;

    public void listen(
            @NonNull FirebaseFirestore db,
            @NonNull String parentUid,
            @NonNull String childId,
            @NonNull Callback cb
    ) {
        DocumentReference ref = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId);

        reg = ref.addSnapshotListener((snap, err) -> {
            if (err != null) { cb.onError(err); return; }
            if (snap == null || !snap.exists()) return;
            cb.onFlags(ShareFlags.from(snap));
        });
    }

    public void stop() {
        if (reg != null) reg.remove();
        reg = null;
    }
}