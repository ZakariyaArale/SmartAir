package com.example.smartairsetup.medlog;


/*
Wrapper Class to store both child name and medication
Used when both are needed, for example when building med inventory list

 */
public class ChildMedicationWrapper {
    private String childName;

    private String childID;
    private Medication medication;

    public ChildMedicationWrapper(String childName, String childID, Medication medication) {
        this.childName = childName;
        this.medication = medication;
        this.childID = childID;
    }



    public String getChildName() {
        return childName;
    }

    public String getChildID(){
        return childID;
    }

    public Medication getMed(){
        return this.medication;
    }
}

