package com.example.truyendex.data.remote;

import android.net.Uri;

import com.example.truyendex.data.model.WalletTx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiService {
    // Sample beneficiary for classroom scanning only.
    private static final String BANK_CODE = "VCB";
    private static final String ACCOUNT = "0000000000";

    private ApiService() {}

    public static String demoQrUrl(WalletTx order) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.vietqr.io/v2/banks").openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            JSONArray banks = new JSONObject(json.toString()).getJSONArray("data");
            for (int i = 0; i < banks.length(); i++) {
                JSONObject bank = banks.getJSONObject(i);
                if (BANK_CODE.equals(bank.getString("code"))) {
                    return Uri.parse("https://img.vietqr.io/image").buildUpon()
                            .appendPath(bank.getString("bin") + "-" + ACCOUNT + "-qr_only.svg")
                            .appendQueryParameter("amount", String.valueOf(order.amount()))
                            .appendQueryParameter("addInfo", order.reference).build().toString();
                }
            }
            throw new IOException("Missing demo bank");
        } finally {
            connection.disconnect();
        }
    }
}
