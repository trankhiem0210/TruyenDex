package com.example.truyendex;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.truyendex.features.reader.ReaderActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class WalletScreenTest {
    @Test
    public void reader_topUpAndUnlockBundle() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        assumeTrue(context.getSharedPreferences("wallet_guest", Context.MODE_PRIVATE).getAll().isEmpty());
        Intent intent = new Intent(context, ReaderActivity.class)
                .putExtra("novel_id", 2).putExtra("chapter_number", 16);
        try (ActivityScenario<ReaderActivity> screen = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnReaderWallet)).perform(click());
            onView(withId(R.id.packOne)).perform(click());
            onView(withId(R.id.confirmDemo)).perform(scrollTo(), click());
            onView(withId(R.id.btnReaderWallet)).check(matches(withText("30 xu")));
            onView(withId(R.id.btnUnlockBundle)).perform(scrollTo(), click());
            onView(withId(R.id.btnReaderWallet)).check(matches(withText("5 xu")));
        } finally {
            context.deleteSharedPreferences("wallet_guest");
        }
    }
}
