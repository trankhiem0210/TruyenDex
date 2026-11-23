package com.example.truyendex.features.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.example.truyendex.R;
import com.example.truyendex.data.model.WalletTx;
import com.example.truyendex.data.remote.ApiService;
import com.example.truyendex.data.repository.WalletRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TopUpBottomSheet extends BottomSheetDialogFragment {
    private ExecutorService worker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.sheet_top_up, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle state) {
        WalletRepository wallet = new WalletRepository(requireContext());
        WalletTx order = wallet.pending();
        if (order == null) {
            dismiss();
            return;
        }
        worker = Executors.newSingleThreadExecutor();
        TextView summary = view.findViewById(R.id.orderSummary);
        summary.setText(String.format(new Locale("vi", "VN"), "Chờ thanh toán · %d xu · %,d đồng\nMã đơn: %s",
                order.coins(), order.amount(), order.reference));
        view.findViewById(R.id.confirmDemo).setOnClickListener(v -> {
            boolean credited = wallet.settleDemo(order.reference, order.amount());
            Toast.makeText(requireContext(), credited ? "Đã cộng " + order.coins() + " xu" : "Đơn đã đóng",
                    Toast.LENGTH_SHORT).show();
            getParentFragmentManager().setFragmentResult(WalletBottomSheet.UPDATED, new Bundle());
            dismiss();
        });
        view.findViewById(R.id.retryQr).setOnClickListener(v -> loadQr(view, order));
        loadQr(view, order);
    }

    private void loadQr(View view, WalletTx order) {
        TextView status = view.findViewById(R.id.qrStatus);
        View retry = view.findViewById(R.id.retryQr);
        WebView qr = view.findViewById(R.id.orderQr);
        status.setText("Đang tải mã VietQR…");
        retry.setVisibility(View.GONE);
        qr.setVisibility(View.INVISIBLE);
        Runnable failed = () -> {
            if (view != getView()) return;
            status.setText("Chưa tải được mã QR. Kiểm tra kết nối rồi thử lại.");
            retry.setVisibility(View.VISIBLE);
            qr.setVisibility(View.INVISIBLE);
        };
        worker.execute(() -> {
            try {
                String url = ApiService.demoQrUrl(order);
                view.post(() -> {
                    if (view != getView()) return;
                    qr.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onReceivedError(WebView browser, WebResourceRequest req, WebResourceError err) {
                            failed.run();
                        }

                        @Override
                        public void onReceivedHttpError(WebView browser, WebResourceRequest req, WebResourceResponse res) {
                            failed.run();
                        }

                        @Override
                        public void onPageFinished(WebView browser, String page) {
                            if (view != getView() || retry.getVisibility() == View.VISIBLE) return;
                            qr.setVisibility(View.VISIBLE);
                            status.setText("");
                        }
                    });
                    String html = "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                            "<body style='margin:0;background:white'><img style='width:100%' src='" +
                            android.text.TextUtils.htmlEncode(url) + "'></body>";
                    qr.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                });
            } catch (Exception e) {
                view.post(failed);
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (worker != null) worker.shutdownNow();
        super.onDestroyView();
    }
}
