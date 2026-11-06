package com.example.truyendex.features.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.truyendex.R;
import com.example.truyendex.data.repository.AuthRepository;
import com.example.truyendex.features.auth.LoginActivity;
import com.google.firebase.auth.FirebaseUser;

public class AccountFragment extends Fragment {

    private final AuthRepository authRepository = new AuthRepository();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = authRepository.getCurrentUser();
        String email = currentUser == null ? "" : currentUser.getEmail();
        String displayName = currentUser == null ? "Người đọc" : getDisplayName(email);

        ((TextView) view.findViewById(R.id.textDisplayName)).setText(displayName);
        ((TextView) view.findViewById(R.id.textAccountEmail)).setText(email);
        ((TextView) view.findViewById(R.id.textAvatarInitials))
                .setText(displayName.substring(0, 1).toUpperCase());

        setPlaceholderAction(view, R.id.buttonEditProfile);
        setPlaceholderAction(view, R.id.buttonChangeAvatar);
        setPlaceholderAction(view, R.id.buttonChangeEmail);
        setPlaceholderAction(view, R.id.buttonChangePassword);

        view.findViewById(R.id.buttonLogout).setOnClickListener(button -> {
            authRepository.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private String getDisplayName(String email) {
        if (email == null || email.isEmpty()) {
            return "Người đọc";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private void setPlaceholderAction(View root, int buttonId) {
        root.findViewById(buttonId).setOnClickListener(button ->
                Toast.makeText(
                        requireContext(),
                        "Tính năng này sẽ được bổ sung sau.",
                        Toast.LENGTH_SHORT
                ).show());
    }
}
