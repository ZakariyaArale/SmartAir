package com.example.smartairsetup.child_home_ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.smartairsetup.navigation.AbstractNavigation;
import com.example.smartairsetup.badges.ChildBadgesActivity;
import com.example.smartairsetup.checkin.PrePostCheckActivity;
import com.example.smartairsetup.R;
import com.example.smartairsetup.notification.AlertHelper;
import com.example.smartairsetup.notification.NotificationPermissionsHelper;
import com.example.smartairsetup.notification.NotificationReceiver;
import com.example.smartairsetup.technique.TechniqueTraining;
import com.example.smartairsetup.triage.RedFlagsActivity_Child;
import com.example.smartairsetup.zone.ZoneActivityChild;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.smartairsetup.notification.AlertRepository;
import com.example.smartairsetup.notification.NotificationPermissionsHelper;
import com.example.smartairsetup.notification.NotificationReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChildHomeActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView greetingText;
    private TextView techniqueStreakText;
    private TextView controllerStreakText;

    private String parentUid;
    private String childId;

    private static final long SIXTY_DAYS_MS = 60L * 24L * 60L * 60L * 1000L;

    private final SimpleDateFormat dayFormatter =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Case 1: parent/provider logged in with FirebaseAuth
        if (currentUser != null) {
            parentUid = currentUser.getUid();
        }
        // Case 2: child login (no FirebaseAuth user, rely on intent extras)
        else if (intent != null) {
            parentUid = intent.getStringExtra("PARENT_UID");
        }

        if (parentUid == null || parentUid.isEmpty()) {
            Toast.makeText(this, "Missing parent id.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Child id is always passed via intent
        if (intent != null) {
            childId = intent.getStringExtra("CHILD_ID");
            if (childId == null || childId.isEmpty()) {
                childId = intent.getStringExtra("CHILD_DOC_ID"); // backward compatibility
            }
        }

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child id is missing.", Toast.LENGTH_SHORT).show();
            return;
        }


        CollectionReference childAccountsRef = db.collection("childAccounts");


        childAccountsRef
                .whereEqualTo("childDocId", childId)
                .whereEqualTo("parentUid", parentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Boolean firstTime = doc.getBoolean("firstTime");
                            if (firstTime != null && firstTime) {
                                // Update firstTime to false
                                doc.getReference()
                                        .update("firstTime", false)
                                        .addOnSuccessListener(aVoid ->
                                                Log.d("ChildHome", "firstTime set to false"))
                                        .addOnFailureListener(e ->
                                                Log.e("ChildHome", "Failed to update firstTime", e));

                                // Optional: notify the child
                                Toast.makeText(this, "Welcome! First time setup.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e("ChildHome", "No matching child found in childAccounts");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ChildHome", "Failed to query childAccounts", e));

        // Continue normal initialization
        greetingText = findViewById(R.id.greetingText);
        techniqueStreakText = findViewById(R.id.techniqueStreakText);
        controllerStreakText = findViewById(R.id.controllerStreakText);

        loadChild();
        setButtons();


    }


    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_child_home;
    }

    private void loadChild() {
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        setGreeting(name);

                        // After child is confirmed, load streaks
                        loadControllerStreak();
                        loadTechniqueStreak();
                    } else {
                        Toast.makeText(this, "Child not found.", Toast.LENGTH_SHORT).show();
                        techniqueStreakText.setText("Your technique-completed streak is: 0");
                        controllerStreakText.setText("Your controller-day streak is: 0");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load child.", Toast.LENGTH_SHORT).show();
                    techniqueStreakText.setText("Your technique-completed streak is: 0");
                    controllerStreakText.setText("Your controller-day streak is: 0");
                });
    }

    private void setButtons() {
        ImageButton buttonBadges = findViewById(R.id.buttonBadges);
        buttonBadges.setOnClickListener(v -> {
            if (childId == null || childId.isEmpty()) {
                Toast.makeText(
                        ChildHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent newIntent = new Intent(ChildHomeActivity.this, ChildBadgesActivity.class);
            newIntent.putExtra("CHILD_ID", childId);
            startActivity(newIntent);
        });

        ImageButton takeMedicationButton = findViewById(R.id.buttonTakeMedication);
        takeMedicationButton.setOnClickListener(v -> {
            if (childId == null || childId.isEmpty()) {
                Toast.makeText(
                        ChildHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent newIntent = new Intent(ChildHomeActivity.this, PrePostCheckActivity.class);
            newIntent.putExtra("CHILD_ID", childId);
            newIntent.putExtra("mode", "pre");
            startActivity(newIntent);
        });

        ImageButton checkZoneButton = findViewById(R.id.buttonCheckZone);
        checkZoneButton.setOnClickListener(v -> {
            if (childId == null || childId.isEmpty() || parentUid == null || parentUid.isEmpty()) {
                Toast.makeText(
                        ChildHomeActivity.this,
                        "Missing child or parent ID.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            Intent zoneIntent = new Intent(ChildHomeActivity.this, ZoneActivityChild.class);
            zoneIntent.putExtra("CHILD_ID", childId);
            zoneIntent.putExtra("PARENT_UID", parentUid);
            startActivity(zoneIntent);
        });

        ImageButton alertButton = findViewById(R.id.notificationButton);
        alertButton.setOnClickListener(v ->{
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(
                    ChildHomeActivity.this,
                    "Please add a child first.",
                    Toast.LENGTH_SHORT
            ).show();
        }
            //a helper method that deals with sending alerts
            AlertHelper.sendAlertToParent(parentUid, childId, "This is a test Alert!", this);
        });

    }
    private void setGreeting(String name) {
        if (name != null && !name.isEmpty()) {
            String message = "Hi, " + name;
            greetingText.setText(message);
        } else {
            Toast.makeText(this, "Child name is empty.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadControllerStreak() {
        long now = System.currentTimeMillis();
        long cutoff = now - SIXTY_DAYS_MS;

        CollectionReference medLogsRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medLogs");

        // Get all med logs, filter in Java
        medLogsRef
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Boolean> controllerDays = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Boolean isRescue = doc.getBoolean("isRescue");
                        if (isRescue == null || isRescue) {
                            continue; // only controller meds
                        }

                        Long tsRaw = doc.getLong("timestamp");
                        if (tsRaw == null) {
                            continue;
                        }

                        long ts = normalizeTimestamp(tsRaw);
                        if (ts < cutoff) {
                            continue;
                        }

                        Date date = new Date(ts);
                        String dayKey = dayFormatter.format(date);
                        controllerDays.put(dayKey, Boolean.TRUE);
                    }

                    int streak = computeConsecutiveDayStreak(controllerDays);
                    controllerStreakText.setText(
                            "Your controller-day streak is: " + streak
                    );
                })
                .addOnFailureListener(e ->
                        controllerStreakText.setText("Your controller-day streak is: 0")
                );
    }

    private void loadTechniqueStreak() {
        long now = System.currentTimeMillis();
        long cutoff = now - SIXTY_DAYS_MS;

        CollectionReference techniqueRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("techniqueLogs");

        techniqueRef
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Boolean> techniqueDays = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Long tsRaw = doc.getLong("timestamp");
                        if (tsRaw == null) {
                            continue;
                        }

                        long ts = normalizeTimestamp(tsRaw);
                        if (ts < cutoff) {
                            continue;
                        }

                        Date date = new Date(ts);
                        String dayKey = dayFormatter.format(date);
                        techniqueDays.put(dayKey, Boolean.TRUE);
                    }

                    int streak = computeConsecutiveDayStreak(techniqueDays);
                    techniqueStreakText.setText(
                            "Your technique-completed streak is: " + streak
                    );
                })
                .addOnFailureListener(e ->
                        techniqueStreakText.setText("Your technique-completed streak is: 0")
                );
    }

    // Convert seconds → ms if needed
    private long normalizeTimestamp(Long tsRaw) {
        long ts = tsRaw;
        if (ts < 10_000_000_000L) {
            // looks like seconds since epoch
            ts = ts * 1000L;
        }
        return ts;
    }

    private int computeConsecutiveDayStreak(Map<String, Boolean> daysWithEvents) {
        if (daysWithEvents == null || daysWithEvents.isEmpty()) {
            return 0;
        }

        Calendar cal = Calendar.getInstance();

        // First check if today has an event
        String todayKey = dayFormatter.format(cal.getTime());
        Boolean todayEvent = daysWithEvents.get(todayKey);

        // If there is NO event today, start counting from yesterday
        if (todayEvent == null || !todayEvent) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        int streak = 0;
        while (true) {
            String key = dayFormatter.format(cal.getTime());
            Boolean hasEvent = daysWithEvents.get(key);

            if (hasEvent != null && hasEvent) {
                streak = streak + 1;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    @Override
    protected void onHomeClicked() {
        //Do nothing as we are in home page
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(this, ChildFamilyActivity.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("PARENT_UID", parentUid);
        }
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {
        Intent intent = new Intent(this, RedFlagsActivity_Child.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("PARENT_UID", parentUid);
        }
        AlertHelper.sendAlertToParent(parentUid, childId, "TRIAGE_START", this);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        Intent intent = new Intent(ChildHomeActivity.this, ChildSettingsActivity.class);
        if (childId != null && !childId.isEmpty()) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("PARENT_UID", parentUid);
        }
        startActivity(intent);
    }
}