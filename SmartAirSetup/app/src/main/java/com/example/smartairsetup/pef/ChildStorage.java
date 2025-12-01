package com.example.smartairsetup.pef;

import com.example.smartairsetup.triage.OptionalEntry;

import java.util.HashMap;
import java.util.Map;

public class ChildStorage {

    private final Map<String, StorageChild> storage = new HashMap<>();
    private final Map<String, OptionalEntry> storage_o = new HashMap<>();

    public void save(String child, StorageChild entry) {

        storage.put(child, entry);
    }

    public void save(String child, OptionalEntry entry) {

        storage_o.put(child, entry);
    }

    public Map<String, StorageChild> getAll() {

        return storage;
    }
}