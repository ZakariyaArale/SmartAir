package com.example.smartairsetup.provider_home_ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartairsetup.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProviderChartsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private LineChart chartSymptoms;

    private String childId;
    private String parentUid;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_trend);

        db = FirebaseFirestore.getInstance();

        parentUid = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_PARENT_UID);
        childId = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_ID);
        String childName = getIntent().getStringExtra(ProviderChildPortalActivity.EXTRA_CHILD_NAME);

        if (parentUid == null || childId == null) {
            Toast.makeText(this, "Missing parent/child info", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView textTrendChildName = findViewById(R.id.textTrendChildName);
        RadioGroup radioRange = findViewById(R.id.radioRange);
        chartSymptoms = findViewById(R.id.chartSymptoms);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());
        textTrendChildName.setText("Child: " + (childName != null ? childName : "Child"));

        radioRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio7days) loadAndPlot(7);
            else if (checkedId == R.id.radio30days) loadAndPlot(30);
        });

        loadAndPlot(7);
    }

    private void loadAndPlot(int days) {
        db.collection("users")
                .document(parentUid)
                .collection("dailyCheckins")
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Integer> scorePerDate = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String dateStr = doc.getString("date");
                        if (dateStr == null) continue;

                        String night = doc.getString("nightWaking");
                        String activity = doc.getString("activityLimits");
                        String cough = doc.getString("coughWheeze");

                        int score = 0;
                        if (isProblem(night)) score++;
                        if (isProblem(activity)) score++;
                        if (isProblem(cough)) score++;

                        Integer existing = scorePerDate.get(dateStr);
                        if (existing == null || score > existing) {
                            scorePerDate.put(dateStr, score);
                        }
                    }

                    buildChartFromScores(scorePerDate, days);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load check-ins: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private boolean isProblem(String value) {
        if (value == null) return false;
        value = value.toLowerCase(Locale.getDefault());
        return value.contains("some") || value.contains("a_lot");
    }

    private void buildChartFromScores(Map<String, Integer> scorePerDate, int days) {
        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        for (int i = days - 1; i >= 0; i--) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.setTime(today);
            dayCal.add(Calendar.DAY_OF_YEAR, -i);

            String dateStr = dateFormat.format(dayCal.getTime());
            int score = scorePerDate.getOrDefault(dateStr, 0);

            int xIndex = days - 1 - i;
            entries.add(new Entry(xIndex, score));
            xLabels.add(dateStr.substring(5)); // "MM-dd"
        }

        LineDataSet dataSet = new LineDataSet(entries, "Problem symptom areas per day (0–3)");
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);

        chartSymptoms.setData(new LineData(dataSet));

        XAxis xAxis = chartSymptoms.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(xLabels.size(), true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        chartSymptoms.getAxisLeft().setAxisMinimum(0f);
        chartSymptoms.getAxisLeft().setAxisMaximum(3f);
        chartSymptoms.getAxisRight().setEnabled(false);

        chartSymptoms.getDescription().setText("Daily symptom burden");
        chartSymptoms.invalidate();
    }
}