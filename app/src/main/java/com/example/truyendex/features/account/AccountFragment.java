package com.example.truyendex.features.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.truyendex.R;
import com.example.truyendex.data.repository.AuthRepository;
import com.example.truyendex.features.auth.LoginActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        String savedDisplayName = currentUser == null ? null : currentUser.getDisplayName();
        String displayName = savedDisplayName == null || savedDisplayName.trim().isEmpty()
                ? getFallbackDisplayName(email)
                : savedDisplayName;

        bindDisplayName(view, displayName);
        ((TextView) view.findViewById(R.id.textAccountEmail)).setText(email);

        view.findViewById(R.id.buttonEditProfile).setOnClickListener(
                button -> showEditDisplayNameDialog(view, displayName));
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

    private String getFallbackDisplayName(String email) {
        if (email == null || email.isEmpty()) {
            return "Người đọc";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private void bindDisplayName(View root, String displayName) {
        ((TextView) root.findViewById(R.id.textDisplayName)).setText(displayName);
        ((TextView) root.findViewById(R.id.textAvatarInitials))
                .setText(displayName.substring(0, 1).toUpperCase());
    }

    private void showEditDisplayNameDialog(View root, String currentDisplayName) {
        EditText nameInput = new EditText(requireContext());
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setHint("Tên hiển thị");
        nameInput.setText(currentDisplayName);
        nameInput.setSelection(nameInput.getText().length());
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        nameInput.setPadding(32, 24, 32, 24);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa tên hiển thị")
                .setView(nameInput)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String updatedName = nameInput.getText().toString().trim();
                    if (updatedName.isEmpty()) {
                        nameInput.setError("Tên hiển thị không được để trống.");
                        return;
                    }

                    button.setEnabled(false);
                    authRepository.updateDisplayName(updatedName)
                            .addOnCompleteListener(requireActivity(), task -> {
                                button.setEnabled(true);
                                if (task.isSuccessful()) {
                                    bindDisplayName(root, updatedName);
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(
                                            requireContext(),
                                            AuthRepository.getErrorMessage(task.getException()),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                }));
        dialog.show();
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
