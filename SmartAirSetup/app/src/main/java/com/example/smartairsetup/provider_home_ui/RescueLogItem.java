package com.example.smartairsetup.provider_home_ui;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

public class RescueLogItem {
    public final String logId;
    public final String medId;
    public final int doseCount;
    public final Date takenAt;

    // hydrated later from /medications/{medId}
    public String medName = "Rescue medication";

    public RescueLogItem(String logId, String medId, int doseCount, Date takenAt) {
        this.logId = logId;
        this.medId = medId;
        this.doseCount = doseCount;
        this.takenAt = takenAt;
    }

    public static RescueLogItem from(DocumentSnapshot doc) {
        String medId = firstNonEmpty(
                doc.getString("MED_ID"),
                doc.getString("medId"),
                doc.getString("medID"),
                doc.getString("medicationId")
        );

        int doseCount = parseIntLike(
                doc.get("DOSE_COUNT"),
                doc.get("doseCount"),
                doc.get("dose")
        );

        Date takenAt = parseAnyTimestamp(doc);

        return new RescueLogItem(doc.getId(), medId, doseCount, takenAt);
    }

    private static Date parseAnyTimestamp(DocumentSnapshot doc) {
        Object raw = doc.get("timestamp");
        if (raw == null) raw = doc.get("TIME_STAMP");
        if (raw == null) raw = doc.get("takenAt");

        if (raw instanceof Timestamp) return ((Timestamp) raw).toDate();
        if (raw instanceof Date) return (Date) raw;
        if (raw instanceof Long) return new Date((Long) raw);
        if (raw instanceof Integer) return new Date(((Integer) raw).longValue());

        if (raw instanceof String) {
            Log.w("RescueLogItem", "timestamp is String: " + raw);
            return null;
        }

        Log.w("RescueLogItem", "timestamp unexpected type: " + (raw == null ? "null" : raw.getClass()));
        return null;
    }

    private static int parseIntLike(Object... vals) {
        for (Object v : vals) {
            if (v == null) continue;
            if (v instanceof Long) return ((Long) v).intValue();
            if (v instanceof Integer) return (Integer) v;
            if (v instanceof Double) return ((Double) v).intValue();
            if (v instanceof String) {
                try { return Integer.parseInt(((String) v).trim()); }
                catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }
}