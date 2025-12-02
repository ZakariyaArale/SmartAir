package com.example.smartairsetup.provider_home_ui;

import com.google.firebase.firestore.DocumentSnapshot;

public class PefLogItem {
    public final String date;
    public final long dailyPEF;
    public final long prePEF;
    public final long postPEF;
    public final long pb;
    public final String zone;
    public final long timestamp;

    public PefLogItem(String date, long dailyPEF, long prePEF, long postPEF, long pb, String zone, long timestamp) {
        this.date = date;
        this.dailyPEF = dailyPEF;
        this.prePEF = prePEF;
        this.postPEF = postPEF;
        this.pb = pb;
        this.zone = zone;
        this.timestamp = timestamp;
    }

    public static PefLogItem from(DocumentSnapshot doc) {
        String date = doc.getString("date");
        String zone = doc.getString("zone");

        long dailyPEF = getLong(doc, "dailyPEF");
        long prePEF = getLong(doc, "prePEF");
        long postPEF = getLong(doc, "postPEF");
        long pb = getLong(doc, "pb");
        long ts = getLong(doc, "timestamp");

        return new PefLogItem(date, dailyPEF, prePEF, postPEF, pb, zone, ts);
    }

    private static long getLong(DocumentSnapshot doc, String field) {
        Object raw = doc.get(field);
        if (raw instanceof Long) return (Long) raw;
        if (raw instanceof Integer) return ((Integer) raw).longValue();
        if (raw instanceof Double) return ((Double) raw).longValue();
        return 0L;
    }
}