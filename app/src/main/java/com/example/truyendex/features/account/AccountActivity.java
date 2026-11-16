package com.example.truyendex.features.account;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WindowInsetsHelper;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.accountRoot));

        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.accountFragmentContainer, new AccountFragment())
                    .commit();
        }
    }
}
