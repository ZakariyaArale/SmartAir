package com.example.smartairsetup.pef;

public class StorageChild {
    private final int dailyPEF;
    private final int prePEF;
    private final int postPEF;

    public StorageChild(int dailyPEF, int prePEF, int postPEF) {
        this.dailyPEF = dailyPEF;
        this.prePEF = prePEF;
        this.postPEF = postPEF;
    }

    public int getDailyPEF() {

        return dailyPEF;
    }
    public int getPrePEF() {

        return prePEF;
    }
    public int getPostPEF() {
        return postPEF;
    }
}
