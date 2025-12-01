package com.example.smartairsetup;

public class MedicationLogEntry {

    public final String childName;
    public final String medName;
    public final String dateTime;
    public final String medType;
    public final int doseCount;
    public final Integer preFeeling;
    public final Integer postFeeling;
    public final String feelingChange;

    public MedicationLogEntry(String childName,
                              String medName,
                              String dateTime,
                              String medType,
                              int doseCount,
                              Integer preFeeling,
                              Integer postFeeling,
                              String feelingChange) {
        this.childName = childName;
        this.medName = medName;
        this.dateTime = dateTime;
        this.medType = medType;
        this.doseCount = doseCount;
        this.preFeeling = preFeeling;
        this.postFeeling = postFeeling;
        this.feelingChange = feelingChange;
    }
}
