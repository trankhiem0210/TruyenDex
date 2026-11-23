package com.example.truyendex.features.account;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.truyendex.R;
import com.example.truyendex.data.repository.AuthRepository;
import com.example.truyendex.features.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        String savedDisplayName = currentUser == null ? null : currentUser.getDisplayName();
        String displayName = savedDisplayName == null || savedDisplayName.trim().isEmpty()
                ? getFallbackDisplayName(email)
                : savedDisplayName;

        bindDisplayName(view, displayName);
        ((TextView) view.findViewById(R.id.textAccountEmail)).setText(email);
        bindSignInMethod(view);

        view.findViewById(R.id.buttonEditDisplayName).setOnClickListener(
                button -> showEditDisplayNameDialog(view, displayName));
        View changePasswordButton = view.findViewById(R.id.buttonChangePassword);
        if (authRepository.hasEmailPasswordProvider()) {
            changePasswordButton.setOnClickListener(button -> showChangePasswordDialog());
        }

        view.findViewById(R.id.buttonLogout).setOnClickListener(button -> {
            authRepository.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void bindSignInMethod(View root) {
        boolean usesGoogle = authRepository.hasGoogleProvider();
        boolean usesEmailPassword = authRepository.hasEmailPasswordProvider();

        String signInMethod;
        if (usesGoogle && usesEmailPassword) {
            signInMethod = "Đăng nhập bằng Google và email/mật khẩu";
        } else if (usesGoogle) {
            signInMethod = "Đăng nhập bằng Google";
        } else if (usesEmailPassword) {
            signInMethod = "Đăng nhập bằng email/mật khẩu";
        } else {
            signInMethod = "Không xác định được phương thức đăng nhập";
        }
        ((TextView) root.findViewById(R.id.textSignInMethod)).setText(signInMethod);

        TextView passwordInfo = root.findViewById(R.id.textPasswordManagementInfo);
        View changePasswordButton = root.findViewById(R.id.buttonChangePassword);
        if (usesEmailPassword) {
            changePasswordButton.setVisibility(View.VISIBLE);
            passwordInfo.setVisibility(View.GONE);
        } else {
            changePasswordButton.setVisibility(View.GONE);
            passwordInfo.setVisibility(View.VISIBLE);
            passwordInfo.setText(usesGoogle
                    ? "Mật khẩu do Google quản lý. Hãy đổi mật khẩu trong tài khoản Google."
                    : "Tài khoản này hiện không có mật khẩu để đổi trong ứng dụng.");
        }
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
                .setTitle("Đổi tên hiển thị")
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

    private void showChangePasswordDialog() {
        LinearLayout fields = new LinearLayout(requireContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.spacing_lg);
        fields.setPadding(
                horizontalPadding,
                0,
                horizontalPadding,
                getResources().getDimensionPixelSize(R.dimen.spacing_sm)
        );

        EditText currentPasswordInput = createPasswordInput("Mật khẩu hiện tại");
        EditText newPasswordInput = createPasswordInput("Mật khẩu mới (ít nhất 6 ký tự)");
        EditText confirmPasswordInput = createPasswordInput("Nhập lại mật khẩu mới");
        fields.addView(currentPasswordInput, passwordFieldLayoutParams());
        fields.addView(newPasswordInput, passwordFieldLayoutParams());
        fields.addView(confirmPasswordInput, passwordFieldLayoutParams());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(fields)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (currentPassword.isEmpty()) {
                        currentPasswordInput.setError("Nhập mật khẩu hiện tại.");
                        return;
                    }
                    if (newPassword.length() < 6) {
                        newPasswordInput.setError("Mật khẩu cần có ít nhất 6 ký tự.");
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        confirmPasswordInput.setError("Mật khẩu xác nhận chưa khớp.");
                        return;
                    }
                    if (currentPassword.equals(newPassword)) {
                        newPasswordInput.setError("Mật khẩu mới phải khác mật khẩu hiện tại.");
                        return;
                    }

                    button.setEnabled(false);
                    dialog.setCancelable(false);
                    authRepository.changePassword(currentPassword, newPassword)
                            .addOnCompleteListener(requireActivity(), task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(
                                            requireContext(),
                                            "Đổi mật khẩu thành công.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    dialog.dismiss();
                                } else {
                                    button.setEnabled(true);
                                    dialog.setCancelable(true);
                                    Toast.makeText(
                                            requireContext(),
                                            AuthRepository.getPasswordChangeErrorMessage(
                                                    task.getException()),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                }));
        dialog.show();
        dialog.setOnDismissListener(ignored -> {
            currentPasswordInput.setText("");
            newPasswordInput.setText("");
            confirmPasswordInput.setText("");
        });
    }

    private EditText createPasswordInput(String hint) {
        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setBackgroundResource(R.drawable.auth_input_background);
        input.setPadding(
                getResources().getDimensionPixelSize(R.dimen.spacing_md),
                0,
                getResources().getDimensionPixelSize(R.dimen.spacing_md),
                0
        );
        input.setSaveEnabled(false);
        return input;
    }

    private LinearLayout.LayoutParams passwordFieldLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.touch_target)
        );
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        return params;
    }

}
