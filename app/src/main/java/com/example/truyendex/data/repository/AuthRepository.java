package com.example.truyendex.data.repository;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public Task<AuthResult> register(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> signIn(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<Void> sendEmailVerification() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No signed-in user to verify."));
        }
        return user.sendEmailVerification();
    }

    public Task<Void> reloadCurrentUser() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No signed-in user to reload."));
        }
        return user.reload();
    }

    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public static String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được đăng ký.";
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu cần có ít nhất 6 ký tự.";
        }
        if (exception instanceof FirebaseAuthInvalidUserException
                || exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không chính xác.";
        }
        if (exception instanceof FirebaseNetworkException) {
            return "Không có kết nối mạng. Vui lòng thử lại.";
        }
        if (exception instanceof FirebaseTooManyRequestsException) {
            return "Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại.";
        }
        return "Không thể hoàn tất yêu cầu. Vui lòng thử lại sau.";
    }
}
