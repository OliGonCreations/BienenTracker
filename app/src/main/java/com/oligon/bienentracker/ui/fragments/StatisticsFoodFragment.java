package com.oligon.bienentracker.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.StatisticsFood;
import com.oligon.bienentracker.ui.activities.StatisticsActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class StatisticsFoodFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private static boolean MAGIC_FLAG = false;

    private StatisticsFood mStats;

    private Typeface tf;
    private Spinner mYearSelector;
    private CheckBox mOrphans;
    private RadioGroup mRangeSelector;
    private BarChart mFoodChart;
    private View mNoData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_food, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mYearSelector = (Spinner) view.findViewById(R.id.statistics_range_year_value);

        mOrphans = (CheckBox) view.findViewById(R.id.statistics_food_orphans);
        mOrphans.setOnCheckedChangeListener(this);

        mRangeSelector = (RadioGroup) view.findViewById(R.id.statistics_range_selector);
        mRangeSelector.setOnCheckedChangeListener(this);

        mFoodChart = (BarChart) view.findViewById(R.id.statistics_food_bar);
        mNoData = view.findViewById(R.id.statistics_food_nodata);

        tf = Typeface.DEFAULT_BOLD;

        setupBarChart(mFoodChart);
        setupRangeSelector();

        try {
            ((RadioButton) getView().findViewById(StatisticsActivity.sp.getInt("stats_selectedfoodrange", R.id.statistics_range_all)))
                    .setChecked(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StatisticsActivity.sp.getInt("stats_selectedfoodrange", R.id.statistics_range_all) == R.id.statistics_range_all) {
            setRangeAll();
            MAGIC_FLAG = false;
        } else {
            mYearSelector.setSelection(StatisticsActivity.sp.getInt("stats_selectedfoodyear", 0));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        StatisticsActivity.sp.edit()
                .putInt("stats_selectedfoodrange", mRangeSelector.getCheckedRadioButtonId())
                .putInt("stats_selectedfoodyear", mYearSelector.getSelectedItemPosition())
                .apply();
    }

    private void setupRangeSelector() {
        int first = StatisticsActivity.db.getFirstEntryDate().get(Calendar.YEAR);
        int last = StatisticsActivity.db.getLastEntryDate().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = last; i >= first; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> mRangeAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, years);
        mRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mYearSelector.setAdapter(mRangeAdapter);
        mYearSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (MAGIC_FLAG) {
                    setRangeYear();
                }
                MAGIC_FLAG = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.statistics_range_all:
                setRangeAll();
                break;
            case R.id.statistics_range_year:
                setRangeYear();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mYearSelector.getVisibility() == View.VISIBLE) {
            setRangeYear();
        } else {
            setRangeAll();
        }
    }

    private void setupBarChart(BarChart bar) {
        bar.setDrawBarShadow(false);
        bar.setDrawValueAboveBar(true);
        bar.setDescription("");
        bar.setPinchZoom(false);
        bar.setExtraOffsets(10, 10, 10, 10);
        bar.setPinchZoom(false);
        bar.setScaleEnabled(false);

        XAxis xAxis = bar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(tf);
        xAxis.setDrawGridLines(false);
        xAxis.setSpaceBetweenLabels(2);
        xAxis.setDrawLabels(false);
        xAxis.setValueFormatter(new XAxisValueFormatter() {
            @Override
            public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
                return original;
            }
        });

        YAxis leftAxis = bar.getAxisLeft();
        leftAxis.setTypeface(tf);
        leftAxis.setLabelCount(4, false);
        leftAxis.setValueFormatter(new YAxisValueFormatter() {
            private DecimalFormat mFormat = new DecimalFormat("###,###,##0");

            @Override
            public String getFormattedValue(float value, YAxis yAxis) {
                return mFormat.format(value) + " kg/l";
            }
        });
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinValue(0f);

        bar.getAxisRight().setEnabled(false);

        Legend l = bar.getLegend();
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setFormSize(9f);
        l.setTypeface(tf);
        l.setTextSize(11f);
        l.setXEntrySpace(8f);
        l.setWordWrapEnabled(true);
    }

    private void setRangeAll() {
        mYearSelector.setVisibility(View.INVISIBLE);
        Calendar first = StatisticsActivity.db.getFirstEntryDate();
        Calendar last = StatisticsActivity.db.getLastEntryDate();
        mStats = StatisticsActivity.db.getStatisticsFood(first, last, mOrphans.isChecked());
        updateBarCharts();
    }

    private void setRangeYear() {
        mYearSelector.setVisibility(View.VISIBLE);
        Calendar last = StatisticsActivity.getZeroCalendar(Integer.parseInt(mYearSelector.getSelectedItem().toString()) + 1);
        Calendar first = StatisticsActivity.getZeroCalendar(Integer.parseInt(mYearSelector.getSelectedItem().toString()));
        last.add(Calendar.MINUTE, -1);
        mStats = StatisticsActivity.db.getStatisticsFood(first, last, mOrphans.isChecked());
        updateBarCharts();
    }

    private void updateBarCharts() {
        ArrayList<BarEntry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> entry : mStats.getFoodStats().entrySet()) {
            yVals.add(new BarEntry(entry.getValue().floatValue(), i, entry.getKey()));
            xVals.add(entry.getKey());
            i++;
        }

        if (i == 0) {
            mFoodChart.setVisibility(View.INVISIBLE);
            mNoData.setVisibility(View.VISIBLE);
            return;
        } else {
            mFoodChart.setVisibility(View.VISIBLE);
            mNoData.setVisibility(View.INVISIBLE);
        }

        BarDataSet dataSet = new BarDataSet(yVals, "");
        dataSet.setBarSpacePercent(30f);
        dataSet.setColors(StatisticsActivity.COLORS_SECONDARY);
        dataSet.setHighLightAlpha(0);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);


        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);
        data.setValueTypeface(tf);

        mFoodChart.setData(data);
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c = 0; c < xVals.size(); c++) {
            colors.add(StatisticsActivity.COLORS_SECONDARY.get(c % StatisticsActivity.COLORS_SECONDARY.size()));
        }
        mFoodChart.getLegend().setCustom(colors, xVals);
        mFoodChart.invalidate();
    }
}
