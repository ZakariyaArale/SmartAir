package com.example.smartairsetup.provider_home_ui;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

public class TriageIncidentItem {
    public final String dateStr;      // "2025-11-25"
    public final String zone;         // "YELLOW" / "RED"
    public final Long dailyPEF;
    public final Long pb;
    public final Long prePEF;
    public final Long postPEF;
    public final Date takenAt;

    public TriageIncidentItem(
            String dateStr,
            String zone,
            Long dailyPEF,
            Long pb,
            Long prePEF,
            Long postPEF,
            Date takenAt
    ) {
        this.dateStr = dateStr;
        this.zone = zone;
        this.dailyPEF = dailyPEF;
        this.pb = pb;
        this.prePEF = prePEF;
        this.postPEF = postPEF;
        this.takenAt = takenAt;
    }

    public static TriageIncidentItem from(DocumentSnapshot doc) {
        String date = doc.getString("date");
        String zone = doc.getString("zone");

        Long dailyPEF = doc.getLong("dailyPEF");
        Long pb = doc.getLong("pb");
        Long pre = doc.getLong("prePEF");
        Long post = doc.getLong("postPEF");

        Date ts = null;
        Object raw = doc.get("timestamp");
        if (raw instanceof Long) {
            ts = new Date((Long) raw);
        }

        // If zone is missing, skip
        if (zone == null || zone.trim().isEmpty()) return null;

        return new TriageIncidentItem(date, zone, dailyPEF, pb, pre, post, ts);
    }
}