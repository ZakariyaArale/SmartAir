package com.example.smartairsetup.notification;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public interface RescueCheckCallback {
    void onResult(boolean triggered);
}
