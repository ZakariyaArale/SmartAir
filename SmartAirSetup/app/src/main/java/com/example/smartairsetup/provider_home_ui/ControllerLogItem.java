package com.example.smartairsetup.provider_home_ui;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.Map;

public class ControllerLogItem {
    public final String medName;
    public final String doseText;
    public final Date takenAt;

    public ControllerLogItem(String medName, String doseText, Date takenAt) {
        this.medName = medName;
        this.doseText = doseText;
        this.takenAt = takenAt;
    }

    public static ControllerLogItem from(
            DocumentSnapshot doc,
            Map<String, String> medIdToName,
            Map<String, Boolean> medIdToIsRescue
    ) {
        // If the log itself has an isRescue field, respect it first:
        Boolean isRescueLog = doc.getBoolean("isRescue");
        if (Boolean.TRUE.equals(isRescueLog)) return null;

        String medId = firstNonEmpty(
                doc.getString("medId"),
                doc.getString("MED_ID"),
                doc.getString("medicationId"),
                doc.getString("medicationID")
        );

        // If log doesn't store isRescue but medId exists, use medication doc knowledge:
        if (medId != null && medIdToIsRescue != null) {
            Boolean isRescueFromMed = medIdToIsRescue.get(medId);
            if (Boolean.TRUE.equals(isRescueFromMed)) return null;
        }

        String name = null;
        if (medId != null && medIdToName != null) {
            name = medIdToName.get(medId);
        }
        if (name == null) {
            name = firstNonEmpty(doc.getString("medName"), doc.getString("medicationName"), doc.getString("name"));
        }
        if (name == null) name = "Controller medication";

        // Dose fields: your flow uses DOSE_COUNT — most teams save it as doseCount.
        String dose = null;
        Long doseCount = doc.getLong("doseCount");
        if (doseCount == null) doseCount = doc.getLong("DOSE_COUNT");

        if (doseCount != null) {
            dose = String.valueOf(doseCount);
        } else {
            dose = firstNonEmpty(doc.getString("dose"), doc.getString("dosage"), doc.getString("amount"), doc.getString("puffs"));
        }
        if (dose == null) dose = "-";

        Date takenAt = parseAnyTimestamp(doc, "timestamp");

        return new ControllerLogItem(name, dose, takenAt);
    }

    private static Date parseAnyTimestamp(DocumentSnapshot doc, String field) {
        Object raw = doc.get(field);
        if (raw == null) raw = doc.get("takenAt");

        if (raw instanceof Timestamp) return ((Timestamp) raw).toDate();
        if (raw instanceof Date) return (Date) raw;
        if (raw instanceof Long) return new Date((Long) raw);

        if (raw != null) {
            Log.w("ControllerLogItem", "Unexpected timestamp type: " + raw.getClass().getName() + " value=" + raw);
        }
        return null;
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }
}