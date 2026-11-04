
package com.example.truyendex;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView btnMenu, btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        btnMenu = findViewById(R.id.btnMenu);
        btnLogin = findViewById(R.id.btnLogin);

        // Menu
        btnMenu.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "Mở menu",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Đăng nhập
        btnLogin.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "Đăng nhập",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}
