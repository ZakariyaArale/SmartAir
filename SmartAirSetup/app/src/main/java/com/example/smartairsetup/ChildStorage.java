package com.example.smartairsetup;

import java.util.HashMap;
import java.util.Map;

public class ChildStorage {

    private final Map<String, StorageChild> storage = new HashMap<>();

    public void save(String child, StorageChild entry) {

        storage.put(child, entry);
    }

    public Map<String, StorageChild> getAll() {

        return storage;
    }
}