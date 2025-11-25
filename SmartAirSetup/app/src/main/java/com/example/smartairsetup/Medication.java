package com.example.smartairsetup;

import java.time.LocalDate;
import java.util.Arrays;

public class Medication implements DateCalculations{

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
    private int [] reminderDays; // a list of how many days ahead of time they want to be reminded
    private int puffNearEmptyThreshold; // should send reminder when puffs is less than this amount

    public Medication(){
        //default constructor needed for firebase
    }

    public Medication(String med_UUID, String name, int purchaseDay, int purchaseMonth, int purchaseYear,
                      int expiryDay, int expiryMonth, int expiryYear,  int[] reminderDays,
                      int puffsLeft, int puffNearEmptyThreshold){

        this.med_UUID = med_UUID;   //need this for firebase
        this.name = name;

        this.purchaseDay = purchaseDay;
        this.purchaseMonth = purchaseMonth;
        this.purchaseYear = purchaseYear;

        this.expiryDay = expiryDay;
        this.expiryMonth = expiryMonth;
        this.expiryYear =expiryYear;

        this.reminderDays = Arrays.copyOf(reminderDays, reminderDays.length);

        this.puffsLeft = puffsLeft;
        this.puffNearEmptyThreshold = puffNearEmptyThreshold;
    }

    public int daysTillExpired(){

        LocalDate date = LocalDate.now(); //gets current date using systems internal clock

        return daysDifference(date.getDayOfMonth(), date.getMonthValue(), date.getYear(),
                this.expiryDay, this.expiryMonth, this.expiryYear);
    }

    public String getMed_UUID(){
        return this.med_UUID;
    }

    public String getName(){
        return this.name;
    }

    public int getPurchaseDay(){
        return this.purchaseDay;
    }

    public int getPurchaseMonth(){
        return this.purchaseMonth;
    }

    public int getPurchaseYear(){
        return this.purchaseYear;
    }

    public int getExpiryDay(){
        return this.expiryDay;
    }
    public int getExpiryMonth(){
        return this.expiryMonth;
    }
    public int getExpiryYear(){
        return this.expiryYear;
    }
    public int [] getReminderDays(){
        return Arrays.copyOf(this.reminderDays, this.reminderDays.length);
    }

    public int getPuffsLeft(){
        return this.puffsLeft;
    }
    public int getPuffNearEmptyThreshold(){
        return puffNearEmptyThreshold;
    }

    public void recordPuffsTaken(int puffsTaken){
        this.puffsLeft -= puffsTaken;

        /*
        if(this.puffsLeft <= puffNearEmptyThreshold){
            //send notification tk

        }*/

    }

}
