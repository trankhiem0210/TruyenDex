package com.example.truyendex.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.truyendex.data.model.WalletTx;
import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

public final class WalletRepository {
    private final SharedPreferences prefs;

    public WalletRepository(Context context) {
        this(context, FirebaseAuth.getInstance().getUid());
    }

    WalletRepository(Context context, String uid) {
        String name = uid == null ? "wallet_guest" : "wallet_uid_" + uid;
        prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public int getBalance() { return prefs.getInt("balance", 0); }

    public WalletTx pending() {
        synchronized (prefs) {
            String ref = prefs.getString("pending", null);
            return ref == null ? null : new WalletTx(ref, prefs.getInt("packs", 1));
        }
    }

    public WalletTx createOrder(int packs) {
        synchronized (prefs) {
            WalletTx existing = pending();
            if (existing != null) return existing;
            String reference = "TDX" + UUID.randomUUID().toString().replace("-", "");
            WalletTx order = new WalletTx(reference, packs);
            prefs.edit().putString("pending", reference).putInt("packs", packs).apply();
            return order;
        }
    }

    public boolean settleDemo(String reference, int amount) {
        synchronized (prefs) {
            WalletTx order = pending();
            if (order == null || !order.reference.equals(reference) || order.amount() != amount) return false;
            prefs.edit().remove("pending").remove("packs")
                    .putInt("balance", getBalance() + order.coins()).apply();
            return true;
        }
    }

    private String key(int novel, int chapter) { return "chapter_" + novel + "_" + chapter; }

    public boolean isUnlocked(int novel, int chapter, boolean paid) {
        return !paid || prefs.getBoolean(key(novel, chapter), false);
    }

    public int bundlePrice(int novel) {
        int price = 25;
        for (int ch = 16; ch <= 18; ch++) {
            if (isUnlocked(novel, ch, true)) price -= 10;
        }
        return Math.max(0, price);
    }

    public boolean unlockSingle(int novel, int chapter) {
        synchronized (prefs) {
            if (isUnlocked(novel, chapter, true)) return true;
            if (getBalance() < 10) return false;
            prefs.edit().putBoolean(key(novel, chapter), true).putInt("balance", getBalance() - 10).apply();
            return true;
        }
    }

    public boolean unlockBundle16To18(int novel) {
        synchronized (prefs) {
            int price = bundlePrice(novel);
            if (getBalance() < price) return false;
            SharedPreferences.Editor edit = prefs.edit().putInt("balance", getBalance() - price);
            for (int ch = 16; ch <= 18; ch++) edit.putBoolean(key(novel, ch), true);
            edit.apply();
            return true;
        }
    }
}
