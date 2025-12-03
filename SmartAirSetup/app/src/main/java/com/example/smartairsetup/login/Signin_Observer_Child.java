package com.example.smartairsetup.login;

public interface Signin_Observer_Child {
    public interface ChildSignInCallback {
        void onSuccess(String parentUid, String childDocId, boolean firstTime);

        void onFailure(String errorMessage);
    }
}
