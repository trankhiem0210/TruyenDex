package com.example.truyendex;

import android.content.Intent;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.truyendex.features.account.AccountActivity;
import com.example.truyendex.features.reader.ReaderActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class ReaderNavigationTest {
    @Test
    public void libraryCard_opensDetailsAndInlineChapterPicker() {
        Intent openChapter = new Intent(ApplicationProvider.getApplicationContext(), ReaderActivity.class)
                .putExtra("novel_id", 2)
                .putExtra("chapter_number", 1);
        try (ActivityScenario<ReaderActivity> reader = ActivityScenario.launch(openChapter)) {
            onView(withId(R.id.tvTopChapter)).check(matches(withText("Chương 1 · Tập 1")));
        }
        try (ActivityScenario<AccountActivity> host = ActivityScenario.launch(AccountActivity.class)) {
            host.onActivity(activity -> {
                NavHostFragment nav = NavHostFragment.create(R.navigation.nav_graph);
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.accountFragmentContainer, nav).commitNow();
                nav.getNavController().navigate(R.id.nav_library);
            });
            onView(allOf(withId(R.id.tvLibTitle), withText("Hẹn em ở mùa hạ năm ấy"))).check(matches(isDisplayed()));
            onView(withId(R.id.tabLibHistory)).perform(click());
            onView(allOf(withId(R.id.tvLibTitle), withText("Hẹn em ở mùa hạ năm ấy"))).check(matches(isDisplayed()));
            onView(withId(R.id.tabLibReading)).perform(click());
            onView(allOf(withId(R.id.tvLibTitle), withText("Hẹn em ở mùa hạ năm ấy"))).perform(click());
            onView(withId(R.id.tvNovelTitle)).check(matches(withText("Hẹn em ở mùa hạ năm ấy")));
            onView(withId(R.id.cardChapters)).perform(scrollTo(), click());
            onView(withId(R.id.chapterDropdown)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.cardChapters)).perform(scrollTo(), click());
            onView(withId(R.id.chapterDropdown)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btnReadContinue)).perform(click());
            onView(withId(R.id.btnChapters)).perform(click());
            onData(anything()).inAdapterView(withId(R.id.chapterDropdown)).atPosition(1).perform(click());
            onView(withId(R.id.tvTopChapter)).check(matches(withText("Chương 2 · Tập 1")));
            onView(withId(R.id.btnListen)).perform(click());
            onView(withId(R.id.ttsPlayerBar)).check(matches(isDisplayed()));
            onView(withId(R.id.btnTtsClose)).perform(click());
            onView(withId(R.id.ttsPlayerBar)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btnReaderBack)).perform(click());
        }
    }
}
