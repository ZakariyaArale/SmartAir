package com.example.smartairsetup.sharing;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

public class ShareFlags {
    public final boolean rescueLogs;
    public final boolean controllerSummary;
    public final boolean symptoms;
    public final boolean triggers;
    public final boolean pef;
    public final boolean triageIncidents;
    public final boolean summaryCharts;

    public ShareFlags(
            boolean rescueLogs,
            boolean controllerSummary,
            boolean symptoms,
            boolean triggers,
            boolean pef,
            boolean triageIncidents,
            boolean summaryCharts
    ) {
        this.rescueLogs = rescueLogs;
        this.controllerSummary = controllerSummary;
        this.symptoms = symptoms;
        this.triggers = triggers;
        this.pef = pef;
        this.triageIncidents = triageIncidents;
        this.summaryCharts = summaryCharts;
    }

    public static @NonNull ShareFlags from(@NonNull DocumentSnapshot s) {
        return new ShareFlags(
                Boolean.TRUE.equals(s.getBoolean("shareRescueLogs")),
                Boolean.TRUE.equals(s.getBoolean("shareControllerSummary")),
                Boolean.TRUE.equals(s.getBoolean("shareSymptoms")),
                Boolean.TRUE.equals(s.getBoolean("shareTriggers")),
                Boolean.TRUE.equals(s.getBoolean("sharePEF")),
                Boolean.TRUE.equals(s.getBoolean("shareTriageIncidents")),
                Boolean.TRUE.equals(s.getBoolean("shareSummaryCharts"))
        );
    }
}