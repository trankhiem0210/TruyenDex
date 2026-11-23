package com.example.truyendex.features.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import com.example.truyendex.R;
import com.example.truyendex.data.repository.WalletRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class WalletBottomSheet extends BottomSheetDialogFragment {
    public static final String UPDATED = "wallet_updated";

    public static void open(FragmentManager manager) {
        new WalletBottomSheet().show(manager, "wallet");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        View view = inflater.inflate(R.layout.sheet_wallet, parent, false);
        WalletRepository wallet = new WalletRepository(requireContext());
        TextView balance = view.findViewById(R.id.walletBalance);
        balance.setText(wallet.getBalance() + " xu");
        Button first = view.findViewById(R.id.packOne);
        Button second = view.findViewById(R.id.packTwo);
        if (wallet.pending() != null) {
            first.setText("Tiếp tục đơn đang chờ");
            second.setVisibility(View.GONE);
        }
        first.setOnClickListener(v -> openOrder(wallet, 1));
        second.setOnClickListener(v -> openOrder(wallet, 2));
        return view;
    }

    private void openOrder(WalletRepository wallet, int packs) {
        wallet.createOrder(packs);
        new TopUpBottomSheet().show(getParentFragmentManager(), "top_up");
        dismiss();
    }
}
