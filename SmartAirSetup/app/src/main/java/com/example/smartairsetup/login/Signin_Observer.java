package com.example.smartairsetup.login;

public interface Signin_Observer {
    // Callback are used to pass results back to the presenter, make sure data is sent correctly
    public interface SignInCallback {
        void onSuccess(String uid, String role);

        void onFailure(String errorMessage);
    }
}
