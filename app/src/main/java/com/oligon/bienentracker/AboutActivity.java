package com.oligon.bienentracker;


import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.oligon.bienentracker.util.AppCompatPreferenceActivity;

public class AboutActivity extends AppCompatPreferenceActivity {


    private static int clickCounter;
    private static boolean isBee, beeAlreadyShowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupActionBar();
        getFragmentManager().beginTransaction()
                .replace(R.id.content, new AboutPreferenceFragment())
                .commit();

        isBee = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("UserIsBee", false);
        beeAlreadyShowed = false;
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.pref_about);
            findPreference("prefs_about_version").setSummary(BuildConfig.VERSION_NAME);
            findPreference("prefs_about_version").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickCounter++;

                    if (!beeAlreadyShowed && isBee && clickCounter > 4) {
                        beeAlreadyShowed = true;
                        Toast.makeText(getActivity(), getString(R.string.easter_egg_activated), Toast.LENGTH_SHORT).show();
                    } else if (!beeAlreadyShowed && clickCounter > 7) {
                        if (!isBee) {
                            Toast.makeText(getActivity(), getString(R.string.easter_egg), Toast.LENGTH_LONG).show();
                            beeAlreadyShowed = true;
                            isBee = true;
                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("UserIsBee", true).apply();
                        }
                    } else if (!beeAlreadyShowed && clickCounter > 4) {
                        if (!isBee) {
                            StringBuilder dots = new StringBuilder();
                            dots.append(" ");
                            for (int i = 3; i < clickCounter; i++) dots.append(".");
                            Toast.makeText(getActivity(), getString(R.string.easter_egg_counter) + dots.toString(), Toast.LENGTH_SHORT).show();


                        }
                    }
                    return false;
                }
            });

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"oligoncreations@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback Bienen Tracker");
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                findPreference("prefs_about_feedback").setIntent(intent);
            }
            findPreference("prefs_about_rate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(i);
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getPackageName())));
                    }
                    return true;
                }
            });
            findPreference("prefs_about_more").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://developer?id=OliGon+Creations"));
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(i);
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/developer?id=OliGon+Creations")));
                    }
                    return true;
                }
            });


        }
    }

}
