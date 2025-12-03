package com.example.smartairsetup.login;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartairsetup.child_home_ui.ChildHomeActivity;
import com.example.smartairsetup.onboarding.OnboardingActivity;
import com.example.smartairsetup.parent_home_ui.ParentHomeActivity;
import com.example.smartairsetup.provider_home_ui.ProviderHomeActivity;
import com.example.smartairsetup.R;

/**
 * View for login screen (handles UI)
 */
public class LoginActivity extends AppCompatActivity implements LoginView {

    private EditText editTextIdentifier, editTextPassword;
    private Button buttonSignIn;
    private TextView textViewError;

    private LoginPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextIdentifier = findViewById(R.id.editTextIdentifier);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSignIn = findViewById(R.id.buttonSignIn);
        Button backButton = findViewById(R.id.backButton);
        textViewError = findViewById(R.id.textViewError);
        TextView textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        presenter = new LoginPresenter(this, new LoginModel(), new BuiltinEmailVerifier());

        buttonSignIn.setOnClickListener(v ->
                presenter.handleSignIn(editTextIdentifier.getText().toString().trim(),
                        editTextPassword.getText().toString())
        );

        textViewForgotPassword.setOnClickListener(v ->
                presenter.handleForgotPassword(editTextIdentifier.getText().toString().trim())
        );

        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public void showError(String message) {
        textViewError.setText(message);
        textViewError.setVisibility(View.VISIBLE);
    }

    @Override
    public void clearError() {
        textViewError.setText("");
        textViewError.setVisibility(View.GONE);
    }

    @Override
    public void enableSignInButton(boolean enable) {
        buttonSignIn.setEnabled(enable);
    }

    @Override
    public void navigateToChildHome(String parentUid, String childId) {
        Intent intent = new Intent(this, ChildHomeActivity.class);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToRoleHome(String role) {
        Intent intent;
        if ("parent".equals(role)) {
            intent = new Intent(this, ParentHomeActivity.class);
        } else if ("provider".equals(role)) {
            intent = new Intent(this, ProviderHomeActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToChildOnboarding(String parentUid, String childId) {
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.putExtra("PARENT_UID", parentUid);
        intent.putExtra("CHILD_ID", childId);
        intent.putExtra("firstTime", true);
        startActivity(intent);
        finish();
    }


    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}