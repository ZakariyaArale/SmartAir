package com.example.smartairsetup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

/**
 * Note that we've used ArgumentCaptor its a bit overkill but we've tested using branch coverage
 * apporach
 */
public class LoginPresenterTest {

    @Mock
    private LoginView mockView;

    @Mock
    private LoginModel mockModel;

    @Mock
    private EmailValidator mockEmailValidator;

    private LoginPresenter presenter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        presenter = new LoginPresenter(mockView, mockModel, mockEmailValidator);
    }

    @Test
    public void handleSignIn_emptyIdentifier_showsError() {
        presenter.handleSignIn("", "password");

        verify(mockView).clearError();
        verify(mockView).showError("Email or username is required");
        verify(mockView, never()).enableSignInButton(anyBoolean());
    }

    @Test
    public void handleSignIn_emptyPassword_showsError() {
        presenter.handleSignIn("user@example.com", "");

        verify(mockView).clearError();
        verify(mockView).showError("Password is required");
        verify(mockView, never()).enableSignInButton(anyBoolean());
    }

    @Test
    public void handleSignIn_invalidEmail_showsError() {
        when(mockEmailValidator.isValid("invalid@email")).thenReturn(false);

        presenter.handleSignIn("invalid@email", "password");

        verify(mockView).clearError();
        verify(mockView).enableSignInButton(false);
        verify(mockView).showError("Enter a valid email");
    }

    @Test
    public void handleSignIn_validParent_callsModelCallback() {
        when(mockEmailValidator.isValid("parent@email.com")).thenReturn(true);

        ArgumentCaptor<LoginModel.SignInCallback> captor = ArgumentCaptor.forClass(LoginModel.SignInCallback.class);

        presenter.handleSignIn("parent@email.com", "password");

        verify(mockView).clearError();
        verify(mockView).enableSignInButton(false);
        verify(mockModel).signInParentOrProvider(eq("parent@email.com"), eq("password"), captor.capture());

        captor.getValue().onSuccess("uid123", "roleParent");
        verify(mockView).navigateToRoleHome("roleParent");

        captor.getValue().onFailure("Sign in failed");
        verify(mockView).enableSignInButton(true);
        verify(mockView).showError("Sign in failed");
    }

    @Test
    public void handleSignIn_child_callsModelCallback() {
        ArgumentCaptor<LoginModel.ChildSignInCallback> captor = ArgumentCaptor.forClass(LoginModel.ChildSignInCallback.class);

        presenter.handleSignIn("childUser", "password");

        verify(mockView).clearError();
        verify(mockView).enableSignInButton(false);
        verify(mockModel).signInChild(eq("childUser"), eq("password"), captor.capture());

        captor.getValue().onSuccess("parentUid123", "childDoc456");
        verify(mockView).navigateToChildHome("parentUid123", "childDoc456");

        captor.getValue().onFailure("Child sign in failed");
        verify(mockView).enableSignInButton(true);
        verify(mockView).showError("Child sign in failed");
    }

    @Test
    public void handleForgotPassword_emptyIdentifier_showsError() {
        presenter.handleForgotPassword("");

        verify(mockView).clearError();
        verify(mockView).showError("Enter your email to reset password");
        verify(mockModel, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPassword_nonEmail_showsError() {
        presenter.handleForgotPassword("childUser");

        verify(mockView).clearError();
        verify(mockView).showError("Password reset is only available for parent/provider emails.");
        verify(mockModel, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPassword_invalidEmail_showsError() {
        when(mockEmailValidator.isValid("invalid@email")).thenReturn(false);

        presenter.handleForgotPassword("invalid@email");

        verify(mockView).clearError();
        verify(mockView).showError("Enter a valid email");
        verify(mockModel, never()).sendPasswordResetEmail(anyString(), any(), any());
    }

    @Test
    public void handleForgotPasswordOnSuccess() {
        when(mockEmailValidator.isValid("good@email.com")).thenReturn(true);

        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<LoginModel.ErrorCallback> errorCaptor =
                ArgumentCaptor.forClass(LoginModel.ErrorCallback.class);

        presenter.handleForgotPassword("good@email.com");

        verify(mockView).clearError();
        verify(mockModel).sendPasswordResetEmail(
                eq("good@email.com"),
                successCaptor.capture(),
                errorCaptor.capture()
        );

        successCaptor.getValue().run();
        verify(mockView).showToast("Password reset email sent. Check your inbox.");
    }

    @Test
    public void handleForgotPasswordOnFailure() {
        when(mockEmailValidator.isValid("good@email.com")).thenReturn(true);

        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<LoginModel.ErrorCallback> errorCaptor =
                ArgumentCaptor.forClass(LoginModel.ErrorCallback.class);

        presenter.handleForgotPassword("good@email.com");

        verify(mockModel).sendPasswordResetEmail(
                eq("good@email.com"),
                successCaptor.capture(),
                errorCaptor.capture()
        );

        errorCaptor.getValue().onError("Reset failed");
        verify(mockView).showError("Reset failed");
    }
}
