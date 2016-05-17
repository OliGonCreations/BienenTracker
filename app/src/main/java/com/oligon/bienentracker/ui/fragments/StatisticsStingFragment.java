package com.oligon.bienentracker.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.util.HiveDB;

import java.util.Calendar;


public class StatisticsStingFragment extends Fragment implements View.OnClickListener {

    private TextView mCounter;
    private BarChart mChart;
    private HiveDB db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_sting, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = HiveDB.getInstance(getActivity());
        ImageButton switchView = (ImageButton) view.findViewById(R.id.statistics_sting_show_stats);
        switchView.setOnClickListener(this);

        Button add = (Button) view.findViewById(R.id.statistics_sting_button_add);
        Button sub = (Button) view.findViewById(R.id.statistics_sting_button_sub);

        add.setOnClickListener(this);
        sub.setOnClickListener(this);

        mCounter = (TextView) view.findViewById(R.id.statistics_sting_counter);
        mChart = (BarChart) view.findViewById(R.id.statistics_sting_chart);

        mCounter.setText(String.valueOf(db.getStingCount()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.statistics_sting_show_stats:
                switchVisibility();
                break;
            case R.id.statistics_sting_button_add:
                db.addSting(Calendar.getInstance().getTime());
                mCounter.setText(String.valueOf(db.getStingCount()));
                break;
            case R.id.statistics_sting_button_sub:
                db.removeLastSting();
                mCounter.setText(String.valueOf(db.getStingCount()));
                break;
        }
    }

    private void switchVisibility() {
        if (mChart.getVisibility() == View.INVISIBLE) {
            mChart.setVisibility(View.VISIBLE);
            mCounter.setVisibility(View.INVISIBLE);
        } else {
            mChart.setVisibility(View.INVISIBLE);
            mCounter.setVisibility(View.VISIBLE);
        }
    }
}
