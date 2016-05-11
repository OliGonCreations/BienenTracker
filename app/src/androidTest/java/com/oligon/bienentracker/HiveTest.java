package com.oligon.bienentracker;

import android.content.res.Resources;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.oligon.bienentracker.ui.activities.HomeActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class HiveTest {

    Resources res;
    private final static String GEN_TITLE = "FancyBees";

    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(HomeActivity.class);

    @Before
    public void setUp() throws Exception {
        res = activityRule.getActivity().getResources();

    }

    @Test
    public void testAddHive() {

        // Click at ActionMenuItemView with id R.id.action_add_hive
        onView(withId(R.id.action_add_hive)).perform(click());

        // Click at menu item with text 'Bienenstock hinzufügen' and id R.id.action_add_hive
        //onView(withText(res.getString(R.string.action_add_hive))).perform(click());

        // Click at AppCompatButton with id android.R.id.button1
        onView(withId(android.R.id.button1)).perform(click());


        // Set text to 'asdf' in AppCompatEditText with id R.id.et_hive_name
        onView(withId(R.id.et_hive_name)).perform(replaceText(GEN_TITLE));
        onView(withId(R.id.et_hive_position)).perform(replaceText("position"));
        onView(withId(R.id.et_hive_year)).perform(replaceText("2015"));
        onView(withId(R.id.et_hive_marker)).perform(replaceText("42"));
        onView(withId(R.id.et_hive_info)).perform(replaceText("information"));
        closeSoftKeyboard();

        // Click at AppCompatButton with id android.R.id.button1
        onView(withId(android.R.id.button1)).perform(click());

        //onRow("asdf").check(matches(isCompletelyDisplayed()));
        onView(withText(GEN_TITLE)).check(matches(isDisplayed()));
        onView(withText("position")).check(matches(isDisplayed()));
        onView(withText("2015")).check(matches(isDisplayed()));

    }



}