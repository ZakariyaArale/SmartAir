package com.example.smartairsetup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListView listChildren;
    private TextView textNoChildren;
    private Button buttonAddChild;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private ArrayAdapter<String> childrenAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button buttonViewHistory = findViewById(R.id.buttonViewHistory);
        buttonViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        Button buttonDailyCheckIn = findViewById(R.id.buttonDailyCheckIn);
        buttonDailyCheckIn.setOnClickListener(v -> {
            if (childIds.isEmpty()) {
                Toast.makeText(ParentHomeActivity.this,
                        "Please add a child first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (childIds.size() == 1) {
                // Only one child – go straight to DailyCheckIn
                launchDailyCheckIn(childIds.get(0), childNames.get(0));
            } else {
                // Multiple children – show a picker dialog
                String[] namesArray = childNames.toArray(new String[0]);

                new AlertDialog.Builder(ParentHomeActivity.this)
                        .setTitle("Select a child")
                        .setItems(namesArray, (dialog, which) -> {
                            if (which >= 0 && which < childIds.size()) {
                                String childId = childIds.get(which);
                                String childName = childNames.get(which);
                                launchDailyCheckIn(childId, childName);
                            }
                        })
                        .show();
            }
        });

        listChildren = findViewById(R.id.listChildren);
        textNoChildren = findViewById(R.id.textNoChildren);
        buttonAddChild = findViewById(R.id.buttonAddChild);

        childrenAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                childNames
        );
        listChildren.setAdapter(childrenAdapter);

        // Go to AddChildActivity when the button is pressed
        buttonAddChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentHomeActivity.this, AddChildActivity.class);
            startActivity(intent);
        });

        // When a child is tapped, open your ShareWithProviderActivity
        listChildren.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= childIds.size()) return;

            String childId = childIds.get(position);
            String childName = childNames.get(position);

            Intent intent = new Intent(ParentHomeActivity.this, ShareWithProviderActivity.class);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_ID, childId);
            intent.putExtra(ShareWithProviderActivity.EXTRA_CHILD_NAME, childName);
            startActivity(intent);
        });

        loadChildren();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list after coming back from AddChildActivity
        loadChildren();
    }

    private void loadChildren() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String parentUid = mAuth.getCurrentUser().getUid();
        CollectionReference childrenRef = db.collection("users")
                .document(parentUid)
                .collection("children");

        childrenRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    childNames.clear();
                    childIds.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) {
                            name = "(Unnamed child)";
                        }
                        childNames.add(name);
                        childIds.add(doc.getId());
                    }

                    childrenAdapter.notifyDataSetChanged();

                    if (childIds.isEmpty()) {
                        textNoChildren.setVisibility(View.VISIBLE);
                        listChildren.setVisibility(View.GONE);
                    } else {
                        textNoChildren.setVisibility(View.GONE);
                        listChildren.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load children: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void launchDailyCheckIn(String childId, String childName) {
        Intent intent = new Intent(ParentHomeActivity.this, DailyCheckIn.class);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_ID, childId);
        intent.putExtra(DailyCheckIn.EXTRA_CHILD_NAME, childName);
        startActivity(intent);
    }

}