package com.example.smartairsetup.medlog;

import com.example.smartairsetup.checkin.DateCalculations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Medication implements DateCalculations {

    //fields
    private String med_UUID; // need this for Firebase?
    private String name;
    private int purchaseDay;
    private int purchaseMonth;
    private int purchaseYear;
    private int puffsLeft; //typically puffers have a specific # of puffs,
    // or fancier will have how many are left. We can keep track by keeping total when first
    // added then subtracting when used
    private int expiryDay;
    private int expiryMonth;
    private int expiryYear;
    private List<Integer> reminderDays; // a list of how many days ahead of time they want to be reminded
    private int puffNearEmptyThreshold; // should send reminder when puffs is less than this amount
    String notes;
    boolean isRescue;

    private Date createdAt;


    public Medication() {
        //default constructor needed for firebase
    }

    public Medication(String med_UUID, String name, int purchaseDay, int purchaseMonth, int purchaseYear,
                      int expiryDay, int expiryMonth, int expiryYear, List<Integer> reminderDays,
                      int puffsLeft, int puffNearEmptyThreshold, String notes, boolean isRescue, Date createdAt) {

        this.med_UUID = med_UUID;   //need this for firebase
        this.name = name;

        this.purchaseDay = purchaseDay;
        this.purchaseMonth = purchaseMonth;
        this.purchaseYear = purchaseYear;

        this.expiryDay = expiryDay;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;

        this.reminderDays = new ArrayList<Integer>(reminderDays);

        this.puffsLeft = puffsLeft;
        this.puffNearEmptyThreshold = puffNearEmptyThreshold;

        this.notes = notes;

        this.isRescue = isRescue;

        this.createdAt = createdAt;
    }

    public int daysTillExpired() {

        LocalDate date = LocalDate.now(); //gets current date using systems internal clock

        return daysDifference(date.getDayOfMonth(), date.getMonthValue(), date.getYear(),
                this.expiryDay, this.expiryMonth, this.expiryYear);
    }

    public String getMed_UUID() {
        return this.med_UUID;
    }

    public void setMed_UUID(String med_UUID) {
        this.med_UUID = med_UUID;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPurchaseDay() {
        return this.purchaseDay;
    }

    public void setPurchaseDay(int purchaseDay) {
        this.purchaseDay = purchaseDay;
    }

    public int getPurchaseMonth() {
        return this.purchaseMonth;
    }

    public void setPurchaseMonth(int purchaseMonth) {
        this.purchaseMonth = purchaseMonth;
    }

    public int getPurchaseYear() {
        return this.purchaseYear;
    }

    public void setPurchaseYear(int purchaseYear) {
        this.purchaseYear = purchaseYear;
    }

    public int getExpiryDay() {
        return this.expiryDay;
    }

    public void setExpiryDay(int expiryDay) {
        this.expiryDay = expiryDay;
    }

    public int getExpiryMonth() {
        return this.expiryMonth;
    }

    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public int getExpiryYear() {
        return this.expiryYear;
    }

    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }

    public List<Integer> getReminderDays() {
        return new ArrayList<>(reminderDays);
    }

    public void setReminderDays(List<Integer> reminderDays) {
        this.reminderDays = new ArrayList<>(reminderDays);
    }

    public int getPuffsLeft() {
        return this.puffsLeft;
    }

    public void setPuffsLeft(int puffsLeft) {
        this.puffsLeft = puffsLeft;
    }

    public int getPuffNearEmptyThreshold() {
        return puffNearEmptyThreshold;
    }

    public void setPuffNearEmptyThreshold(int puffNearEmptyThreshold) {
        this.puffNearEmptyThreshold = puffNearEmptyThreshold;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean getisRescue() {
        return isRescue;
    }

    public void setisRescue(boolean rescue) {
        this.isRescue = rescue;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}