package com.example.truyendex.features.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.truyendex.MainActivity;
import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.repository.AuthRepository;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class LoginActivity extends Activity {

    private final AuthRepository authRepository = new AuthRepository();
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button googleSignInButton;
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
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn);
        verificationPanel = findViewById(R.id.verificationPanel);
        verificationMessage = findViewById(R.id.textVerificationMessage);
        resendVerificationButton = findViewById(R.id.textResendVerification);
        checkVerificationButton = findViewById(R.id.textCheckVerification);

        loginButton.setOnClickListener(view -> signIn());
        googleSignInButton.setOnClickListener(view -> signInWithGoogle());
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

    private void signInWithGoogle() {
        setLoading(true);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(this);
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException exception) {
                        setLoading(false);
                        if (!(exception instanceof GetCredentialCancellationException)) {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Không thể chọn tài khoản Google. Vui lòng thử lại.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );
    }

    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(
                        credential.getType())) {
            setLoading(false);
            Toast.makeText(
                    this,
                    "Loại thông tin đăng nhập Google không hợp lệ.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        GoogleIdTokenCredential googleCredential =
                GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
        authRepository.signInWithGoogleToken(googleCredential.getIdToken())
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        openMainScreen();
                    } else {
                        Toast.makeText(
                                this,
                                AuthRepository.getErrorMessage(task.getException()),
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
        googleSignInButton.setEnabled(!loading);
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
