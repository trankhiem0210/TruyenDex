package com.example.truyendex.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean hasEmailPasswordProvider() {
        return hasProvider(EmailAuthProvider.PROVIDER_ID);
    }

    public boolean hasGoogleProvider() {
        return hasProvider(GoogleAuthProvider.PROVIDER_ID);
    }

    private boolean hasProvider(String providerId) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return false;
        }
        for (UserInfo provider : user.getProviderData()) {
            if (providerId.equals(provider.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public Task<AuthResult> register(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> signIn(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> signInWithGoogleToken(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return firebaseAuth.signInWithCredential(credential);
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

    public Task<Void> updateDisplayName(String displayName) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No signed-in user to update."));
        }

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        return user.updateProfile(profileUpdate);
    }

    public Task<Void> changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No email user is signed in."));
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        return user.reauthenticate(credential).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception != null) {
                    throw exception;
                }
                throw new IllegalStateException("Could not reauthenticate user.");
            }
            return user.updatePassword(newPassword);
        });
    }

    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public static String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được đăng ký. Hãy đăng nhập bằng phương thức đã dùng trước đó.";
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

    public static String getPasswordChangeErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Mật khẩu hiện tại không chính xác.";
        }
        if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại rồi thử lại.";
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu mới chưa đáp ứng yêu cầu của Firebase.";
        }
        return getErrorMessage(exception);
    }
}
