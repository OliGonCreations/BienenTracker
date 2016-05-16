package com.oligon.bienentracker.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.oligon.bienentracker.R;
import com.oligon.bienentracker.ui.fragments.StatisticsEarningsFragment;
import com.oligon.bienentracker.ui.fragments.StatisticsFoodFragment;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.adapter.StatisticsViewPagerAdapter;

import java.util.ArrayList;
import java.util.Calendar;

public class StatisticsActivity extends AppCompatActivity {

    public static ArrayList<Integer> COLORS_PRIMARY = new ArrayList<>();
    public static ArrayList<Integer> COLORS_SECONDARY = new ArrayList<>();

    public static HiveDB db;
    public static SharedPreferences sp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupColors();

        db = HiveDB.getInstance(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        ViewPager viewPager = (ViewPager) findViewById(R.id.statistics_viewpager);
        StatisticsViewPagerAdapter adapter = new StatisticsViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new StatisticsEarningsFragment(), "Ertrag");
        adapter.addFragment(new StatisticsFoodFragment(), "Futter");
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupColors() {
        //COLORS_PRIMARY.add(ContextCompat.getColor(this, R.color.pie1));
        //COLORS_PRIMARY.add(ContextCompat.getColor(this, R.color.pie2));
        COLORS_PRIMARY.add(ContextCompat.getColor(this, R.color.pie3));
        COLORS_PRIMARY.add(ContextCompat.getColor(this, R.color.pie4));
        COLORS_PRIMARY.add(ContextCompat.getColor(this, R.color.pie5));
        COLORS_SECONDARY.add(ContextCompat.getColor(this, R.color.pie6));
        COLORS_SECONDARY.add(ContextCompat.getColor(this, R.color.pie7));
        COLORS_SECONDARY.add(ContextCompat.getColor(this, R.color.pie8));
        COLORS_SECONDARY.add(ContextCompat.getColor(this, R.color.pie9));
    }

    public static Calendar getZeroCalendar(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar;
    }
}
