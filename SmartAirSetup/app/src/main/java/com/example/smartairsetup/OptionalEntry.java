package com.example.smartairsetup;

public class OptionalEntry {
    private final int rescueAttempts;
    private final int pefTriage;

    public OptionalEntry(int rescueAttempts, int pefTriage) {
        this.rescueAttempts = rescueAttempts;
        this.pefTriage = pefTriage;
    }

    public int getRescueAttempts() { return rescueAttempts; }
    public int getPefTriage() { return pefTriage; }
}