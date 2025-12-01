package com.example.smartairsetup.login;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Model for login screen (handles business logic), note some buisness logic relating to
 * db is in the model. This is the only way that we can do Junit tests for presenter
 */
public class LoginModel {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public LoginModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void signInParentOrProvider(String email, String password,
                                       SignInCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        db.collection("users").document(uid)
                                .get()
                                .addOnSuccessListener(document -> {
                                    String role = document != null && document.exists()
                                            ? document.getString("role")
                                            : null;
                                    callback.onSuccess(uid, role);
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        String message = "Sign in failed";
                        if (task.getException() != null) message = task.getException().getMessage();
                        callback.onFailure(message);
                    }
                });
    }

    public void signInChild(String username, String password,
                            ChildSignInCallback callback) {
        db.collection("childAccounts").document(username)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("No child account found with this username.");
                        return;
                    }
                    String storedPassword = doc.getString("password");
                    if (storedPassword == null || !storedPassword.equals(password)) {
                        callback.onFailure("Incorrect password for this child account.");
                        return;
                    }
                    callback.onSuccess(doc.getString("parentUid"), doc.getString("childDocId"));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to sign in child: " + e.getMessage()));
    }

    public void sendPasswordResetEmail(String email, Runnable onSuccess, ErrorCallback onFailure) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        String message = "Failed to send reset email";
                        if (task.getException() != null) message = task.getException().getMessage();
                        onFailure.onError(message);
                    }
                });
    }

    public interface SignInCallback {
        void onSuccess(String uid, String role);
        void onFailure(String errorMessage);
    }

    public interface ChildSignInCallback {
        void onSuccess(String parentUid, String childDocId);
        void onFailure(String errorMessage);
    }

    public interface ErrorCallback {
        void onError(String errorMessage);
    }
}
