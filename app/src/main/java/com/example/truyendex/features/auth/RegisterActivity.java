package com.example.truyendex.features.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.truyendex.MainActivity;
import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.repository.AuthRepository;

public class RegisterActivity extends Activity {

    private final AuthRepository authRepository = new AuthRepository();
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.registerRoot));

        emailInput = findViewById(R.id.editEmail);
        passwordInput = findViewById(R.id.editPassword);
        confirmPasswordInput = findViewById(R.id.editConfirmPassword);
        registerButton = findViewById(R.id.buttonRegister);

        registerButton.setOnClickListener(view -> register());
        findViewById(R.id.textSignIn).setOnClickListener(
                view -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authRepository.getCurrentUser() != null) {
            openMainScreen();
        }
    }

    private void register() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Nhập email hợp lệ.");
            emailInput.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Mật khẩu cần có ít nhất 6 ký tự.");
            passwordInput.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Mật khẩu xác nhận chưa khớp.");
            confirmPasswordInput.requestFocus();
            return;
        }

        setLoading(true);
        authRepository.register(email, password)
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

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Đang tạo tài khoản..." : "Tạo tài khoản");
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
