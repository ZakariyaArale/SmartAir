package com.example.smartairsetup.login;

/**
 * Presenter for login screen (handles business logic), note some buisness logic relating to
 * db is in the model. This is the only way that we can do Junit tests.
 */
public class LoginPresenter {

    private final LoginView view;
    private final LoginModel model;
    private final EmailValidator emailValidator;

    public LoginPresenter(LoginView view, LoginModel model, EmailValidator emailValidator) {
        this.view = view;
        this.model = model;
        this.emailValidator = emailValidator;
    }

    public void handleSignIn(String identifier, String password) {
        view.clearError();

        if (identifier.isEmpty()) {
            view.showError("Email or username is required");
            return;
        }

        if (password.isEmpty()) {
            view.showError("Password is required");
            return;
        }

        view.enableSignInButton(false);

        if (identifier.contains("@")) {
            if (!emailValidator.isValid(identifier)) {
                view.showError("Enter a valid email");
                view.enableSignInButton(true);
                return;
            }

            model.signInParentOrProvider(identifier, password, new LoginModel.SignInCallback() {
                @Override
                public void onSuccess(String uid, String role) {
                    view.navigateToRoleHome(role);
                }

                @Override
                public void onFailure(String errorMessage) {
                    view.enableSignInButton(true);
                    view.showError(errorMessage);
                }
            });

        } else {
            model.signInChild(identifier, password, new LoginModel.ChildSignInCallback() {
                @Override
                public void onSuccess(String parentUid, String childDocId) {
                    view.navigateToChildHome(parentUid, childDocId);
                }

                @Override
                public void onFailure(String errorMessage) {
                    view.enableSignInButton(true);
                    view.showError(errorMessage);
                }
            });
        }
    }

    public void handleForgotPassword(String identifier) {
        view.clearError();

        if (identifier.isEmpty()) {
            view.showError("Enter your email to reset password");
            return;
        }

        if (!identifier.contains("@")) {
            view.showError("Password reset is only available for parent/provider emails.");
            return;
        }

        if (!emailValidator.isValid(identifier)) {
            view.showError("Enter a valid email");
            return;
        }

        model.sendPasswordResetEmail(identifier,
                () -> view.showToast("Password reset email sent. Check your inbox."),
                errorMessage -> view.showError(errorMessage));
    }
}
