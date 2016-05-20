package com.oligon.bienentracker.ui.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.StatisticsEarnings;
import com.oligon.bienentracker.object.Trend;
import com.oligon.bienentracker.ui.activities.StatisticsActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


public class StatisticsEarningsFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private static boolean MAGIC_FLAG = false;
    private static String OTHERS;
    private static final int MAX_HIVES_SHOWN_IN_CHART = 5;

    private StatisticsEarnings mStats;
    private Trend mTrend;
    private ArrayAdapter<String> mRangeAdapter;
    private Typeface tf;

    private PieChart mGroupChart, mGroupDetailChart;
    private BarChart mTrendChart;
    private TextView mGroupSubTitle, mGroupValue;
    private Spinner mYearSelector;
    private CheckBox mOrphans;
    private RadioGroup mRangeSelector;
    private View mTrendView, mGroupsView, mNoDataView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_earnings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        OTHERS = getString(R.string.others);

        mYearSelector = (Spinner) view.findViewById(R.id.statistics_range_year_value);

        mOrphans = (CheckBox) view.findViewById(R.id.statistics_earnings_orphans);
        mOrphans.setOnCheckedChangeListener(this);

        mRangeSelector = (RadioGroup) view.findViewById(R.id.statistics_range_selector);
        mRangeSelector.setOnCheckedChangeListener(this);

        mGroupChart = (PieChart) view.findViewById(R.id.statistics_groups_chart);
        mGroupDetailChart = (PieChart) view.findViewById(R.id.statistics_groups_detail_chart);
        mTrendChart = (BarChart) view.findViewById(R.id.statistics_trend_bar);

        mGroupSubTitle = (TextView) view.findViewById(R.id.statistics_groups_subtitle);
        mGroupValue = (TextView) view.findViewById(R.id.statistics_groups_value);

        mTrendView = view.findViewById(R.id.statistics_trend_card);
        mGroupsView = view.findViewById(R.id.statistics_groups_card);
        mNoDataView = view.findViewById(R.id.statistics_earnings_nodata);

        tf = Typeface.DEFAULT_BOLD;

        setupPieChart(mGroupChart);
        setupPieChart(mGroupDetailChart);
        setupBarChart(mTrendChart);

        mTrend = StatisticsActivity.db.getTrend(mOrphans.isChecked());
        updateBarChart();

        StatisticsActivity.sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setupRangeSelector();
        setRangeAll();

        try {
            ((RadioButton) getView().findViewById(StatisticsActivity.sp.getInt("stats_selectedrange", R.id.statistics_range_all)))
                    .setChecked(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StatisticsActivity.sp.getInt("stats_selectedrange", R.id.statistics_range_all) == R.id.statistics_range_all) {
            setRangeAll();
            MAGIC_FLAG = false;
        } else {
            mYearSelector.setSelection(StatisticsActivity.sp.getInt("stats_selectedyear", 0));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        StatisticsActivity.sp.edit()
                .putInt("stats_selectedrange", mRangeSelector.getCheckedRadioButtonId())
                .putInt("stats_selectedyear", mYearSelector.getSelectedItemPosition())
                .apply();
    }

    private void setRangeAll() {
        mYearSelector.setVisibility(View.INVISIBLE);
        mTrendView.setVisibility(View.VISIBLE);
        mGroupsView.setVisibility(View.GONE);
        mNoDataView.setVisibility(View.GONE);
    }

    private void setRangeYear() {
        mYearSelector.setVisibility(View.VISIBLE);
        mTrendView.setVisibility(View.GONE);
        mNoDataView.setVisibility(View.GONE);
        mGroupsView.setVisibility(View.VISIBLE);
        Calendar last = StatisticsActivity.getZeroCalendar(Integer.parseInt(mYearSelector.getSelectedItem().toString()) + 1);
        Calendar first = StatisticsActivity.getZeroCalendar(Integer.parseInt(mYearSelector.getSelectedItem().toString()));
        last.add(Calendar.MINUTE, -1);
        mStats = StatisticsActivity.db.getStatisticsEarnings(first, last, mOrphans.isChecked());
        updatePieCharts();
    }

    private void setNoData() {
        mNoDataView.setVisibility(View.VISIBLE);
        mTrendView.setVisibility(View.GONE);
        mGroupsView.setVisibility(View.GONE);
    }

    private void updateBarChart() {
        ArrayList<BarEntry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> entry : mTrend.getTrendYear().entrySet()) {
            yVals.add(new BarEntry(entry.getValue().floatValue(), i, entry.getKey()));
            xVals.add(entry.getKey());
            i++;
        }

        if (i == 0) {
            setNoData();
            return;
        }

        BarDataSet dataSet = new BarDataSet(yVals, "");
        dataSet.setBarSpacePercent(10f);
        dataSet.setColors(StatisticsActivity.COLORS_PRIMARY);
        dataSet.setValueFormatter(new WeightFormatter());
        dataSet.setHighLightAlpha(0);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);
        data.setValueTypeface(tf);

        mTrendChart.setData(data);
        mTrendChart.invalidate();
        mTrendChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                ((RadioButton) getView().findViewById(R.id.statistics_range_year)).setChecked(true);
                mYearSelector.setSelection(mRangeAdapter.getPosition(e.getData().toString()), true);
            }

            @Override
            public void onNothingSelected() {

            }
        });
    }

    /*private void setTrendYear() {
        ArrayList<BarEntry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> entry : mTrend.getTrendMonth().get(mYearSelector.getSelectedItem().toString()).entrySet()) {
            yVals.add(new BarEntry(entry.getValue().floatValue(), i, entry.getKey()));
            xVals.add(entry.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(yVals, "");
        dataSet.setBarSpacePercent(10f);
        dataSet.setColors(StatisticsActivity.COLORS_SECONDARY);
        dataSet.setValueFormatter(new WeightFormatter());
        dataSet.setHighLightAlpha(0);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);
        data.setValueTypeface(tf);

        mTrendChart.setData(data);
        mTrendChart.invalidate();
    }*/

    private void setupRangeSelector() {
        int first = StatisticsActivity.db.getFirstEntryDate().get(Calendar.YEAR);
        int last = StatisticsActivity.db.getLastEntryDate().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = last; i >= first; i--) {
            years.add(String.valueOf(i));
        }
        mRangeAdapter = new ArrayAdapter<>(getActivity(),
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

    private void setupPieChart(PieChart pie) {
        pie.setUsePercentValues(false);
        pie.setDrawHoleEnabled(false);
        pie.setRotationEnabled(false);
        pie.setDescription("");
        pie.setExtraOffsets(10, 10, 10, 10);
        //pie.setDragDecelerationFrictionCoef(0.8f);

        Legend l = pie.getLegend();
        l.setEnabled(false);
    }

    private void setupBarChart(BarChart bar) {
        bar.setDrawBarShadow(false);
        bar.setDrawValueAboveBar(true);
        bar.setDescription("");
        bar.setPinchZoom(false);

        XAxis xAxis = bar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(tf);
        xAxis.setDrawGridLines(false);
        xAxis.setSpaceBetweenLabels(2);

        YAxis leftAxis = bar.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = bar.getAxisRight();
        rightAxis.setEnabled(false);

        Legend l = bar.getLegend();
        l.setEnabled(false);

    }

    private void updatePieCharts() {

        if (mStats.getHoneySum() == 0) {
            setNoData();
            return;
        }

        TreeMap<String, Double> groupStats = mStats.getGroupStats();

        // Groupchart
        ArrayList<Entry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> entry : entriesSortedByValues(groupStats)) {
            yVals.add(new Entry(entry.getValue().floatValue(), i, entry.getKey()));
            xVals.add(entry.getKey());
            i++;
        }

        PieDataSet dataSet = new PieDataSet(yVals, "");
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);

        dataSet.setColors(StatisticsActivity.COLORS_PRIMARY);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new WeightFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(tf);
        mGroupChart.setData(data);

        mGroupChart.highlightValue(new Highlight(0, 0), false);
        mGroupChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                updateDetailGroupsChart(e.getData().toString());
            }

            @Override
            public void onNothingSelected() {

            }
        });
        mGroupChart.invalidate();
        if (!xVals.isEmpty())
            updateDetailGroupsChart(xVals.get(0));
        DecimalFormat format = new DecimalFormat("###,###,##0.0");
        mGroupValue.setText(getString(R.string.statistics_earnings, format.format(mStats.getHoneySum())));
    }

    private void updateDetailGroupsChart(String group) {
        mGroupSubTitle.setText(group);

        TreeMap<String, TreeMap<String, Double>> groupDetailStats = mStats.getGroupDetailStats();

        if (!groupDetailStats.containsKey(group)) return;

        TreeMap<String, Double> groupStats = groupDetailStats.get(group);

        ArrayList<Entry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> entry : entriesSortedByValues(groupStats)) {
            yVals.add(new Entry(entry.getValue().floatValue(), i, entry.getKey()));
            xVals.add(entry.getKey());
            i++;
        }

        PieDataSet dataSet = new PieDataSet(yVals, "");
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(0f);

        dataSet.setColors(StatisticsActivity.COLORS_SECONDARY);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new WeightFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        data.setValueTypeface(tf);
        mGroupDetailChart.setData(data);

        mGroupDetailChart.highlightValues(null);

        mGroupDetailChart.invalidate();

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
        mTrend = StatisticsActivity.db.getTrend(isChecked);
        updateBarChart();
        if (mYearSelector.getVisibility() == View.INVISIBLE)
            setRangeAll();
        else setRangeYear();
    }

    public class WeightFormatter implements ValueFormatter, YAxisValueFormatter {

        private DecimalFormat mFormat;

        public WeightFormatter() {
            mFormat = new DecimalFormat("###,###,##0.0");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value) + " kg";
        }

        @Override
        public String getFormattedValue(float value, YAxis yAxis) {
            return mFormat.format(value) + " kg";
        }
    }

    private static SortedSet<Map.Entry<String, Double>> entriesSortedByValues(TreeMap<String, Double> map) {
        SortedSet<Map.Entry<String, Double>> sortedEntries = new TreeSet<>(
                new Comparator<Map.Entry<String, Double>>() {
                    @Override
                    public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                        if (e1.getKey().equals(OTHERS)) return 1;
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        TreeMap<String, Double> tempMap = (TreeMap<String, Double>) map.clone();
        sortedEntries.addAll(tempMap.entrySet());
        if (sortedEntries.size() > MAX_HIVES_SHOWN_IN_CHART) {
            double others = 0;
            while (tempMap.size() > MAX_HIVES_SHOWN_IN_CHART) {
                Map.Entry<String, Double> last = sortedEntries.last();
                others += last.getValue();
                tempMap.remove(last.getKey());
                sortedEntries.clear();
                sortedEntries.addAll(tempMap.entrySet());
            }
            Map<String, Double> temp = new TreeMap<>();
            temp.put(OTHERS, others);
            sortedEntries.addAll(temp.entrySet());
        }
        return sortedEntries;
    }

}
