package com.example.smartairsetup;

/**
 * This interface is a clean way of following SRP and linking the view to the presenter.
 */
public interface LoginView {
    void showError(String message);
    void clearError();
    void enableSignInButton(boolean enable);
    void navigateToChildHome(String parentUid, String childId);
    void navigateToRoleHome(String role);
    void showToast(String message);
}
