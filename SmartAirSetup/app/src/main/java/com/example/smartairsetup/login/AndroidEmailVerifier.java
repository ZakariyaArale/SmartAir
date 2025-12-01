package com.example.smartairsetup.login;

import android.util.Patterns;

public class AndroidEmailVerifier implements EmailValidator {
    @Override
    public boolean isValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
