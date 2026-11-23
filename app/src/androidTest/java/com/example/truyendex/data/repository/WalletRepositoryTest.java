package com.example.truyendex.data.repository;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.truyendex.data.model.WalletTx;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WalletRepositoryTest {
    @Test
    public void pendingOrder_survivesReopenAndCreditsOnlyOnce() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String uid = UUID.randomUUID().toString();
        try {
            WalletRepository wallet = new WalletRepository(context, uid);
            WalletTx order = wallet.createOrder(1);
            WalletRepository reopened = new WalletRepository(context, uid);
            assertEquals(order.reference, reopened.createOrder(2).reference);
            assertFalse(reopened.settleDemo(order.reference, order.amount() + 1));
            assertFalse(reopened.settleDemo("wrong", order.amount()));
            assertFalse(reopened.unlockSingle(2, 16));
            assertTrue(reopened.settleDemo(order.reference, order.amount()));
            assertFalse(wallet.settleDemo(order.reference, order.amount()));
            assertEquals(30, wallet.getBalance());
            assertTrue(wallet.unlockSingle(2, 16));
            assertEquals(15, reopened.bundlePrice(2));
            assertTrue(reopened.unlockBundle16To18(2));
            assertTrue(wallet.unlockBundle16To18(2));
            assertEquals(5, reopened.getBalance());
            assertTrue(wallet.isUnlocked(2, 18, true));
            assertFalse(wallet.isUnlocked(3, 18, true));
            assertFalse(new WalletRepository(context, uid + "_other").isUnlocked(2, 18, true));
        } finally {
            context.deleteSharedPreferences("wallet_uid_" + uid);
            context.deleteSharedPreferences("wallet_uid_" + uid + "_other");
        }
    }
}
