package com.example.smartairsetup;

import java.util.List;

public interface ChildFetchListener {
    void onChildrenLoaded(List<UserID> children);
    void onError(Exception e);
}
