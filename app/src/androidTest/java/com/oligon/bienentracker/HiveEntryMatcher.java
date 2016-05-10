package com.oligon.bienentracker;

import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.support.test.espresso.core.deps.guava.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.is;

public class HiveEntryMatcher {
    static Matcher<View> withTitle(final String substring) {
        return withTitle(is(substring));
    }

    static Matcher<View> withTitle(final Matcher<String> stringMatcher) {
        checkNotNull(stringMatcher);
        return new BoundedMatcher<View, View>(EditText.class) {

            @Override
            public boolean matchesSafely(View view) {
                final CharSequence hint = ((TextView) view.findViewById(R.id.card_hive_name)).getText();
                return hint != null && stringMatcher.matches(hint.toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with hint: ");
                stringMatcher.describeTo(description);
            }
        };
    }
}
