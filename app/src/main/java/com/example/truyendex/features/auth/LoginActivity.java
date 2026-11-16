package com.example.truyendex.features.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.truyendex.MainActivity;
import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.repository.AuthRepository;

public class LoginActivity extends Activity {

    private final AuthRepository authRepository = new AuthRepository();
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private View verificationPanel;
    private TextView verificationMessage;
    private TextView resendVerificationButton;
    private TextView checkVerificationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.loginRoot));

        emailInput = findViewById(R.id.editEmail);
        passwordInput = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.buttonLogin);
        verificationPanel = findViewById(R.id.verificationPanel);
        verificationMessage = findViewById(R.id.textVerificationMessage);
        resendVerificationButton = findViewById(R.id.textResendVerification);
        checkVerificationButton = findViewById(R.id.textCheckVerification);

        loginButton.setOnClickListener(view -> signIn());
        findViewById(R.id.textCreateAccount).setOnClickListener(
                view -> {
                    if (authRepository.getCurrentUser() != null
                            && !authRepository.isEmailVerified()) {
                        authRepository.signOut();
                    }
                    startActivity(new Intent(this, RegisterActivity.class));
                });
        findViewById(R.id.textForgotPassword).setOnClickListener(view -> sendPasswordReset());
        resendVerificationButton.setOnClickListener(view -> resendVerificationEmail());
        checkVerificationButton.setOnClickListener(view -> checkVerificationStatus());
        findViewById(R.id.textUseAnotherAccount).setOnClickListener(view -> useAnotherAccount());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authRepository.isEmailVerified()) {
            openMainScreen();
        } else if (authRepository.getCurrentUser() != null) {
            showVerificationPanel();
        }
    }

    private void signIn() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (!isValidEmail(email)) {
            emailInput.setError("Nhập email hợp lệ.");
            emailInput.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Vui lòng nhập mật khẩu.");
            passwordInput.requestFocus();
            return;
        }

        setLoading(true);
        authRepository.signIn(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        if (authRepository.isEmailVerified()) {
                            openMainScreen();
                        } else {
                            showVerificationPanel();
                        }
                    } else {
                        Exception exception = task.getException();
                        Toast.makeText(
                                this,
                                AuthRepository.getErrorMessage(exception),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showVerificationPanel() {
                String email = authRepository.getCurrentUser().getEmail();
                if (email != null) {
                    emailInput.setText(email);
                }
                verificationMessage.setText(
                        "Email chưa được xác minh. Mở liên kết trong hộp thư của bạn, sau đó quay lại kiểm tra."
                );
                verificationPanel.setVisibility(View.VISIBLE);
    }

    private void resendVerificationEmail() {
                resendVerificationButton.setEnabled(false);
                authRepository.sendEmailVerification()
                        .addOnCompleteListener(this, task -> {
                            resendVerificationButton.setEnabled(true);
                            String message = task.isSuccessful()
                                    ? "Đã gửi lại email xác minh. Hãy kiểm tra hộp thư và thư rác."
                                    : AuthRepository.getErrorMessage(task.getException());
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        });
    }

    private void checkVerificationStatus() {
                checkVerificationButton.setEnabled(false);
                authRepository.reloadCurrentUser()
                        .addOnCompleteListener(this, task -> {
                            checkVerificationButton.setEnabled(true);
                            if (task.isSuccessful() && authRepository.isEmailVerified()) {
                                openMainScreen();
                            } else if (task.isSuccessful()) {
                                Toast.makeText(
                                        this,
                                        "Email vẫn chưa được xác minh. Hãy mở liên kết trong email trước.",
                                        Toast.LENGTH_LONG
                                ).show();
                            } else {
                                Toast.makeText(
                                        this,
                                        AuthRepository.getErrorMessage(task.getException()),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
    }

    private void useAnotherAccount() {
                authRepository.signOut();
                verificationPanel.setVisibility(View.GONE);
                emailInput.setText("");
                passwordInput.setText("");
    }

    private void sendPasswordReset() {
        String email = emailInput.getText().toString().trim();
        if (!isValidEmail(email)) {
            emailInput.setError("Nhập email hợp lệ để đặt lại mật khẩu.");
            emailInput.requestFocus();
            return;
        }

        findViewById(R.id.textForgotPassword).setEnabled(false);
        authRepository.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    findViewById(R.id.textForgotPassword).setEnabled(true);
                    String message = task.isSuccessful()
                            ? "Nếu email đã đăng ký, hướng dẫn đặt lại mật khẩu sẽ được gửi."
                            : AuthRepository.getErrorMessage(task.getException());
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
