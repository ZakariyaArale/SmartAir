package com.example.smartairsetup.parent_home_ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.smartairsetup.navigation.AbstractNavigation;
import com.example.smartairsetup.login.AddChildActivity;
import com.example.smartairsetup.child_home_ui.ChildOverviewActivity;
import com.example.smartairsetup.medlog.ControllerLogActivity;
import com.example.smartairsetup.checkin.DailyCheckIn;
import com.example.smartairsetup.history.HistoryActivity;
import com.example.smartairsetup.medlog.MedicationInventoryActivity;
import com.example.smartairsetup.badges.ParentBadgeSettingsActivity;
import com.example.smartairsetup.R;
import com.example.smartairsetup.notification.NotificationPermissionsHelper;
import com.example.smartairsetup.pb.PBActivity;
import com.example.smartairsetup.pdf.PDFStoreActivity;
import com.example.smartairsetup.pef.PEFActivity;
import com.example.smartairsetup.triage.RedFlagsActivity;
import com.example.smartairsetup.zone.ZoneActivity;
import com.example.smartairsetup.notification.AlertHelper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;

public class ParentHomeActivity extends AbstractNavigation {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button buttonAddChild;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();

    // Child overview UI
    private String parentUid;
    private TextView textWeeklyRescue;
    private Button buttonOverviewSelectChild;
    private String selectedOverviewChildId;
    private ListenerRegistration alertsListener;
    // Shared-with-provider tags (in XML)
    private TextView tagSharedRescue, tagSharedSymptoms, tagSharedHistory, tagSharedPB,
            tagSharedPEF, tagSharedPDF, tagSharedZone, tagSharedController;

    private TextView textLastRescueUsed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Bind shared tags ---
        tagSharedRescue = findViewById(R.id.tagSharedRescue);
        tagSharedSymptoms = findViewById(R.id.tagSharedSymptoms);
        tagSharedHistory = findViewById(R.id.tagSharedHistory);
        tagSharedPB = findViewById(R.id.tagSharedPB);
        tagSharedPEF = findViewById(R.id.tagSharedPEF);
        tagSharedPDF = findViewById(R.id.tagSharedPDF);
        tagSharedZone = findViewById(R.id.tagSharedZone);
        tagSharedController = findViewById(R.id.tagSharedController);

        textLastRescueUsed = findViewById(R.id.textLastRescueUsed);

        hideAllShareTags(); // default state

        // --- Buttons ---
        Button buttonViewHistory = findViewById(R.id.buttonViewHistory);
        buttonViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(ParentHomeActivity.this, HistoryActivity.class));
        });

        Button buttonChildOverview = findViewById(R.id.buttonChildOverview);
        buttonChildOverview.setOnClickListener(v -> {
            startActivity(new Intent(ParentHomeActivity.this, ChildOverviewActivity.class));
        });

        Button buttonSetPB = findViewById(R.id.buttonSetPB);
        buttonSetPB.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, PBActivity.class));
        });

        Button buttonEnterPEF = findViewById(R.id.buttonEnterPEF);
        buttonEnterPEF.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, PEFActivity.class));
        });

        Button buttonAddBadges = findViewById(R.id.buttonAddBadges);
        buttonAddBadges.setOnClickListener(v -> {
            startActivity(new Intent(this, ParentBadgeSettingsActivity.class));
        });

        Button buttonDailyCheckIn = findViewById(R.id.buttonDailyCheckIn);
        buttonDailyCheckIn.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (childIds.size() == 1) {
                launchDailyCheckIn(childIds.get(0), childNames.get(0));
            } else {
                String[] namesArray = childNames.toArray(new String[0]);
                new AlertDialog.Builder(this)
                        .setTitle("Select a child")
                        .setItems(namesArray, (dialog, which) -> {
                            if (which >= 0 && which < childIds.size()) {
                                launchDailyCheckIn(childIds.get(which), childNames.get(which));
                            }
                        })
                        .show();
            }
        });

        buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonAddChild.setOnClickListener(v -> {
            startActivity(new Intent(this, AddChildActivity.class));
        });

        Button buttonMedicationInventory = findViewById(R.id.buttonMedicationInventory);
        buttonMedicationInventory.setOnClickListener(v -> {
            startActivity(new Intent(this, MedicationInventoryActivity.class));
        });

        Button buttonPDF = findViewById(R.id.buttonPDF);
        buttonPDF.setOnClickListener(v -> {
            if (selectedOverviewChildId == null) {
                Toast.makeText(this, "Please select a child first.", Toast.LENGTH_SHORT).show();
                return;
            }

            String childId = selectedOverviewChildId;
            int index = childIds.indexOf(childId);
            String childName = (index >= 0 && index < childNames.size()) ? childNames.get(index) : "(Unnamed child)";

            Intent intent = new Intent(this, PDFStoreActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        });


        Button buttonZone = findViewById(R.id.buttonZone);
        buttonZone.setOnClickListener(v -> {
            if (parentUid == null || parentUid.isEmpty()) {
                Toast.makeText(this, "Parent UID not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ZoneActivity.class);
            intent.putExtra("PARENT_UID", parentUid);
            startActivity(intent);
        });

        Button buttonControllerLog = findViewById(R.id.buttonControllerLog);
        buttonControllerLog.setOnClickListener(v -> {
            startActivity(new Intent(this, ControllerLogActivity.class));
        });

        // --- Child summary card ---
        buttonOverviewSelectChild = findViewById(R.id.buttonOverviewSelectChild);
        textWeeklyRescue = findViewById(R.id.textWeeklyRescue);

        buttonOverviewSelectChild.setEnabled(false);
        buttonOverviewSelectChild.setOnClickListener(v -> showOverviewChildDialog());

        loadChildren();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (alertsListener != null) {
            alertsListener.remove();
            alertsListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren();

        // If we already have a child selected, refresh the tags (in case switches changed)
        loadSharingTagsForSelectedChild();

        //check notification permissions
        NotificationPermissionsHelper.ensureNotificationPermissions(this);
        NotificationPermissionsHelper.ensureAlarmPermissions(this);
    }

    private void loadChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        parentUid = mAuth.getCurrentUser().getUid();

        subscribeToAlertsForParent(parentUid);

        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        childrenRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    childNames.clear();
                    childIds.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = "(Unnamed child)";
                        childNames.add(name);
                        childIds.add(doc.getId());
                    }

                    if (childIds.isEmpty()) {
                        selectedOverviewChildId = null;
                        buttonOverviewSelectChild.setEnabled(false);
                        buttonOverviewSelectChild.setText("No children");
                        textWeeklyRescue.setText("No children found for this parent.");
                        hideAllShareTags();
                    } else {
                        // Do not auto-select
                        selectedOverviewChildId = null;
                        buttonOverviewSelectChild.setEnabled(true);
                        buttonOverviewSelectChild.setText("Select child");
                        textWeeklyRescue.setText("Select a child to view this week's rescue count.");
                        hideAllShareTags();
                    }
                })
                .addOnFailureListener(e -> {
                    selectedOverviewChildId = null;
                    buttonOverviewSelectChild.setEnabled(false);
                    buttonOverviewSelectChild.setText("No children");
                    textWeeklyRescue.setText("Error loading children.");
                    hideAllShareTags();
                    Toast.makeText(this, "Failed to load children: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showOverviewChildDialog() {
        if (childIds.isEmpty()) {
            Toast.makeText(this, "Please add a child first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] namesArray = childNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select a child")
                .setItems(namesArray, (dialog, which) -> {
                    if (which >= 0 && which < childIds.size()) {
                        selectedOverviewChildId = childIds.get(which);
                        String name = childNames.get(which);
                        buttonOverviewSelectChild.setText(name);

                        loadWeeklyRescueForChild(selectedOverviewChildId);
                        loadLastRescueUse(selectedOverviewChildId);
                        loadSharingTagsForSelectedChild(); // <-- key line
                    }
                })
                .show();
    }

    private void loadWeeklyRescueForChild(String childId) {
        if (parentUid == null || childId == null) {
            textWeeklyRescue.setText("Could not load weekly rescue data.");
            return;
        }

        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        textWeeklyRescue.setText("No weekly rescue data for this child.");
                        return;
                    }

                    Long countLong = doc.getLong("weekly_rescue_medication_count");
                    int count = (countLong != null) ? countLong.intValue() : 0;

                    textWeeklyRescue.setText("Rescue medication count this week: " + count);
                })
                .addOnFailureListener(e ->
                        textWeeklyRescue.setText("Could not load weekly rescue data.")
                );
    }

    // ---------------- Shared-with-provider tags ----------------

    private void hideAllShareTags() {
        setTagVisible(tagSharedRescue, false);
        setTagVisible(tagSharedSymptoms, false);
        setTagVisible(tagSharedHistory, false);
        setTagVisible(tagSharedPB, false);
        setTagVisible(tagSharedPEF, false);
        setTagVisible(tagSharedPDF, false);
        setTagVisible(tagSharedZone, false);
        setTagVisible(tagSharedController, false);
    }

    private void setTagVisible(TextView tag, boolean visible) {
        if (tag == null) return;
        tag.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void loadLastRescueUse(String childId) {
        Log.d("LastRescue", "parentUid=" + parentUid + " childId=" + childId);
        db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childId)
                .collection("medLogs")
                .whereEqualTo("isRescue", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        textLastRescueUsed.setText("Last rescue medication use: —");
                        return;
                    }

                    DocumentSnapshot doc = qs.getDocuments().get(0);

                    // timestamp might be Long/Double/Timestamp depending on how you wrote it
                    Object raw = doc.get("timestamp");
                    Long tsMillis = null;

                    if (raw instanceof Long) tsMillis = (Long) raw;
                    else if (raw instanceof Double) tsMillis = ((Double) raw).longValue();
                    else if (raw instanceof Timestamp)
                        tsMillis = ((Timestamp) raw).toDate().getTime();

                    if (tsMillis == null) {
                        textLastRescueUsed.setText("Last rescue medication use: —");
                        return;
                    }

                    DateFormat fmt =
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                    String when = fmt.format(new Date(tsMillis));

                    textLastRescueUsed.setText("Last rescue medication use: " + when);
                })
                .addOnFailureListener(e -> {
                    Log.e("LastRescue", "Failed to load last rescue use", e);
                    textLastRescueUsed.setText("Last rescue medication use: —");
                });
    }

    private void loadSharingTagsForSelectedChild() {
        if (mAuth.getCurrentUser() == null) return;
        if (selectedOverviewChildId == null || parentUid == null) {
            hideAllShareTags();
            return;
        }

        DocumentReference childRef = db.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedOverviewChildId);

        childRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        hideAllShareTags();
                        return;
                    }

                    boolean shareRescueLogs = Boolean.TRUE.equals(snapshot.getBoolean("shareRescueLogs"));
                    boolean shareControllerSummary = Boolean.TRUE.equals(snapshot.getBoolean("shareControllerSummary"));
                    boolean shareSymptoms = Boolean.TRUE.equals(snapshot.getBoolean("shareSymptoms"));
                    boolean shareTriggers = Boolean.TRUE.equals(snapshot.getBoolean("shareTriggers"));
                    boolean sharePEF = Boolean.TRUE.equals(snapshot.getBoolean("sharePEF"));
                    boolean shareTriageIncidents = Boolean.TRUE.equals(snapshot.getBoolean("shareTriageIncidents"));
                    boolean shareSummaryCharts = Boolean.TRUE.equals(snapshot.getBoolean("shareSummaryCharts"));

                    // Reasonable mapping based on what your toggles mean:
                    setTagVisible(tagSharedRescue, shareRescueLogs);

                    // Symptoms bucket includes triggers + triage too
                    setTagVisible(tagSharedSymptoms, shareSymptoms || shareTriggers || shareTriageIncidents);

                    // History is basically logs across things
                    setTagVisible(tagSharedHistory,
                            shareRescueLogs || sharePEF || shareSymptoms || shareTriggers || shareTriageIncidents);

                    // PB affects PEF-derived data
                    setTagVisible(tagSharedPB, sharePEF);

                    setTagVisible(tagSharedPEF, sharePEF);

                    // PDF: assume charts/summary
                    setTagVisible(tagSharedPDF, shareSummaryCharts);

                    // Zone is computed from PEF
                    setTagVisible(tagSharedZone, sharePEF);

                    setTagVisible(tagSharedController, shareControllerSummary);
                })
                .addOnFailureListener(e -> hideAllShareTags());
    }

    private void subscribeToAlertsForParent(String parentUid) {
        // Clean old listener if any (e.g. after role switch / re-login)
        if (alertsListener != null) {
            alertsListener.remove();
            alertsListener = null;
        }

        alertsListener = db.collection("users")
                .document(parentUid)
                .collection("alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("ParentAlerts", "Listener error", e);
                        return;
                    }
                    if (snap == null) return;

                    for (DocumentChange change : snap.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot doc = change.getDocument();

                        Boolean handled = doc.getBoolean("handled");
                        if (Boolean.TRUE.equals(handled)) {
                            continue; // already shown
                        }

                        String type = doc.getString("type");
                        String message = doc.getString("message");
                        if (message == null) message = "Alert from your child.";

                        showAlertNotification(type, message);

                        // Mark as handled so we don't spam the parent
                        doc.getReference().update("handled", true);
                    }
                });
    }

    private void showAlertNotification(String type, String message) {

        if (!NotificationPermissionsHelper.ensureNotificationPermissions(this)) {
            return;
        }
        AlertHelper.showAlert(this, type, message);
    }

    private void launchDailyCheckIn(String childId, String childName) {
        Intent intent = new Intent(ParentHomeActivity.this, DailyCheckIn.class);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_ID, childId);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_NAME, childName);
        startActivity(intent);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_parent_home;
    }

    @Override
    protected void onHomeClicked() {
        // Already on home
    }

    @Override
    protected void onFamilyClicked() {
        Intent intent = new Intent(ParentHomeActivity.this, ParentFamilyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    protected void onEmergencyClicked() {

        Intent intent = new Intent(ParentHomeActivity.this, RedFlagsActivity.class);
        intent.putExtra("PARENT_UID", parentUid);
        startActivity(intent);
    }

    @Override
    protected void onSettingsClicked() {
        startActivity(new Intent(ParentHomeActivity.this, ParentSettingsActivity.class));
    }
}