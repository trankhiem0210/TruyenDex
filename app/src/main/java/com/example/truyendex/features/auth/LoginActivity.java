package com.example.truyendex.features.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.truyendex.MainActivity;
import com.example.truyendex.R;
import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.repository.AuthRepository;

public class LoginActivity extends Activity {

    private final AuthRepository authRepository = new AuthRepository();
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.loginRoot));

        emailInput = findViewById(R.id.editEmail);
        passwordInput = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.buttonLogin);

        loginButton.setOnClickListener(view -> signIn());
        findViewById(R.id.textCreateAccount).setOnClickListener(
                view -> startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.textForgotPassword).setOnClickListener(view -> sendPasswordReset());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authRepository.getCurrentUser() != null) {
            openMainScreen();
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
                        openMainScreen();
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
