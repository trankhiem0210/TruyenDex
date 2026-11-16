
package com.example.truyendex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.repository.AuthRepository;
import com.example.truyendex.features.account.AccountActivity;
import com.example.truyendex.features.auth.LoginActivity;

public class MainActivity extends Activity {

    private final AuthRepository authRepository = new AuthRepository();
    private TextView btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (authRepository.getCurrentUser() == null || !authRepository.isEmailVerified()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.mainRoot));

        TextView btnMenu = findViewById(R.id.btnMenu);
        btnLogin = findViewById(R.id.txtLogin);
        btnLogin.setText("Tài khoản");

        btnMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Mở menu", Toast.LENGTH_SHORT).show();
        });

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));
    }
}
