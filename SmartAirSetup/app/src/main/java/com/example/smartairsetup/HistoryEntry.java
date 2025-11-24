package com.example.smartairsetup;

public class HistoryEntry {
    public final String childName;
    public final String date;
    public final String night;
    public final String activity;
    public final String cough;
    public final String triggers;
    public final String author;

    public HistoryEntry(String childName,
                        String date,
                        String night,
                        String activity,
                        String cough,
                        String triggers,
                        String author) {
        this.childName = childName;
        this.date = date;
        this.night = night;
        this.activity = activity;
        this.cough = cough;
        this.triggers = triggers;
        this.author = author;
    }
}
