package com.example.smartairsetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import com.example.smartairsetup.login.EmailValidator;
import com.example.smartairsetup.login.LoginModel;
import com.example.smartairsetup.login.LoginPresenter;
import com.example.smartairsetup.login.LoginView;

/** IMPORTANT: Video with Rawad uses older version of mockito. We have use the documentation and
 * examples online to get it to work. Additionality since we used imports for email validation
 * it would be tricky to test it using the Mockito methods learning in the MVP lec as such we used
 * interfaces and classes to help with the presenters "buisness logic"
 * */
@RunWith(MockitoJUnitRunner.class)
public class LoginPresenterTest {

    @Mock
    private LoginView view;

    @Mock
    private LoginModel model;

    @Mock
    private EmailValidator builtin_validator;

    private LoginPresenter presenter;

    //This is a method that runs before each test cases
    @Before
    public void setUp() {
        presenter = new LoginPresenter(view, model, builtin_validator);
    }

    @Test
    public void handleSignIn_emptyIdentifier_showsError() {
        presenter.handleSignIn("", "123456");
        verify(view).clearError();
        verify(view).showError("Email or username is required");
        verify(view, never()).enableSignInButton(anyBoolean());
    }

    @Test
    public void handleSignIn_emptyPassword() {
        presenter.handleSignIn("rawad@gmail.com", "");
        verify(view).clearError();
        verify(view).showError("Password is required");
        verify(view, never()).enableSignInButton(anyBoolean());
    }

    @Test
    public void handleSignIn_invalidEmail() {
        when(builtin_validator.isValid("rawad@gmail.com")).thenReturn(false);
        presenter.handleSignIn("rawad@gmail.com", "CSCB07");
        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(view).showError("Enter a valid email");
    }

    @Test
    public void handleSignIn_parent() {
        when(builtin_validator.isValid("rawad@gmail.com")).thenReturn(true);
        ArgumentCaptor<LoginModel.SignInCallback> captor = ArgumentCaptor.forClass(LoginModel.SignInCallback.class);
        presenter.handleSignIn("rawad@gmail.com", "CSCB07");
        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(model).signInParentOrProvider(eq("rawad@gmail.com"), eq("CSCB07"), captor.capture());
        captor.getValue().onSuccess("id", "roleParent");
        verify(view).navigateToRoleHome("roleParent");
    }

    @Test
    public void handleSignIn_parent_Failure() {
        when(builtin_validator.isValid("rawad@gmail.com")).thenReturn(true);
        ArgumentCaptor<LoginModel.SignInCallback> captor = ArgumentCaptor.forClass(LoginModel.SignInCallback.class);
        presenter.handleSignIn("rawad@gmail.com", "CSCB07");
        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(model).signInParentOrProvider(eq("rawad@gmail.com"), eq("CSCB07"), captor.capture());
        captor.getValue().onFailure("Sign in failed");
        verify(view).enableSignInButton(true);
        verify(view).showError("Sign in failed");
    }

    @Test
    public void handleSignIn_child_Onboarding() {
        ArgumentCaptor<LoginModel.ChildSignInCallback> captor = ArgumentCaptor.forClass(LoginModel.ChildSignInCallback.class);
        presenter.handleSignIn("Anna", "CSCB07");
        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(model).signInChild(eq("Anna"), eq("CSCB07"), captor.capture());
        captor.getValue().onSuccess("pid", "cid", true);
        verify(view).navigateToChildOnboarding("pid", "cid");
    }

    @Test
    public void handleSignIn_child_callsModelCallback() {
        ArgumentCaptor<LoginModel.ChildSignInCallback> captor = ArgumentCaptor.forClass(LoginModel.ChildSignInCallback.class);
        presenter.handleSignIn("Anna", "CSCB07");

        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(model).signInChild(eq("Anna"), eq("CSCB07"), captor.capture());

        captor.getValue().onSuccess("pid", "cid", false);
        verify(view).navigateToChildHome("pid", "cid");
    }

    @Test
    public void handleSignIn_child_Failure() {
        ArgumentCaptor<LoginModel.ChildSignInCallback> captor = ArgumentCaptor.forClass(LoginModel.ChildSignInCallback.class);
        presenter.handleSignIn("Anna", "CSCA08");
        verify(view).clearError();
        verify(view).enableSignInButton(false);
        verify(model).signInChild(eq("Anna"), eq("CSCA08"), captor.capture());
        captor.getValue().onFailure("Child sign in failed");
        verify(view).enableSignInButton(true);
        verify(view).showError("Child sign in failed");
    }

    @Test
    public void handleForgotPassword_EmptyError() {
        presenter.handleForgotPassword("");
        verify(view).clearError();
        verify(view).showError("Enter your email to reset password");
        verify(model, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPassword_NoEmailError() {
        presenter.handleForgotPassword("Anna");
        verify(view).clearError();
        verify(view).showError("Password reset is only available for parent/provider emails.");
        verify(model, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPassword_InvalidEmail_showsError() {
        when(builtin_validator.isValid("rawad@gmai.com")).thenReturn(false);
        presenter.handleForgotPassword("rawad@gmai.com");
        verify(view).clearError();
        verify(view).showError("Enter a valid email");
        verify(model, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPasswordOnSuccess() {
        when(builtin_validator.isValid("rawad@gmai.com")).thenReturn(true);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<LoginModel.ErrorCallback> errorCaptor = ArgumentCaptor.forClass(LoginModel.ErrorCallback.class);
        presenter.handleForgotPassword("rawad@gmai.com");
        verify(view).clearError();
        verify(model).sendPasswordResetEmail(
                eq("rawad@gmai.com"),
                successCaptor.capture(),
                errorCaptor.capture()
        );
        successCaptor.getValue().run();
        verify(view).showToast("Password reset email sent. Check your inbox.");
    }

    @Test
    public void handleForgotPasswordOnFailure() {
        when(builtin_validator.isValid("rawad@gmai.com")).thenReturn(true);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<LoginModel.ErrorCallback> errorCaptor = ArgumentCaptor.forClass(LoginModel.ErrorCallback.class);
        presenter.handleForgotPassword("rawad@gmai.com");
        verify(model).sendPasswordResetEmail(eq("rawad@gmai.com"), successCaptor.capture(), errorCaptor.capture());
        errorCaptor.getValue().onError("Reset failed");
        verify(view).showError("Reset failed");
    }
}
