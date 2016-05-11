package com.oligon.bienentracker.util.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.oligon.bienentracker.ui.activities.LogActivity;
import com.oligon.bienentracker.ui.activities.NewEntryActivity;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.LogEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder> implements Filterable {

    private List<LogEntry> mList;
    private List<LogEntry> mFiltered;
    private static Context context;
    private ItemFilter mFilter = new ItemFilter();

    public LogListAdapter(List<LogEntry> list) {
        this.mList = list;
        this.mFiltered = list;
    }

    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context)
                .inflate(R.layout.card_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        LogEntry entry = mFiltered.get(position);
        holder.mEntry = entry;
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());
        try {
            String date = context.getResources().getString(R.string.card_date) + " " + sdf.format(entry.getDate());
            holder.card_title.setText(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (entry.getWeatherCode() != -1) {
            holder.card_weather.setVisibility(View.VISIBLE);
            holder.card_weather_text.setText(entry.getWeatherString(context));
            holder.card_weather_temp.setText(String.format(Locale.getDefault(), "%.1f °C", entry.getTemp()));
            holder.card_weather_img.setImageResource(entry.getWeatherImageId(context));
        } else holder.card_weather.setVisibility(View.GONE);

        if (entry.hasFood()) {
            holder.card_food.setVisibility(View.VISIBLE);
            holder.card_food_text.setText(entry.getFoodText());
        } else holder.card_food.setVisibility(View.GONE);

        if (entry.hasHarvest()) {
            holder.card_harvest.setVisibility(View.VISIBLE);
            holder.card_harvest_text.setText(entry.getHarvestText(context));
        } else holder.card_harvest.setVisibility(View.GONE);

        if (entry.hasTreatment()) {
            holder.card_treatment.setVisibility(View.VISIBLE);
            holder.card_treatment_text.setText(entry.getTreatmentText());
        } else holder.card_treatment.setVisibility(View.GONE);

        if (entry.hasActivities()) {
            holder.card_activity.setVisibility(View.VISIBLE);
            holder.card_activity_text.setText(entry.getActivitiesText(context));
        } else holder.card_activity.setVisibility(View.GONE);

        if (entry.hasInspection()) {
            holder.card_inspection_text.setText(entry.getInspectionText());
            holder.card_inspection.setVisibility(View.VISIBLE);
        } else holder.card_inspection.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public class LogViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView card_title, card_food_text, card_treatment_text,
                card_harvest_text, card_activity_text, card_inspection_text,
                card_weather_temp, card_weather_text;
        public View card_food, card_treatment, card_harvest, card_activity, card_inspection, card_weather;
        public ImageView card_weather_img;
        public ImageButton card_options;

        public LogEntry mEntry;


        public LogViewHolder(View v) {
            super(v);
            card_title = (TextView) v.findViewById(R.id.card_title);
            card_options = (ImageButton) v.findViewById(R.id.card_options);
            card_weather_temp = (TextView) v.findViewById(R.id.card_weather_temp);
            card_weather_text = (TextView) v.findViewById(R.id.card_weather_text);
            card_food_text = (TextView) v.findViewById(R.id.card_food_text);
            card_treatment_text = (TextView) v.findViewById(R.id.card_treatment_text);
            card_harvest_text = (TextView) v.findViewById(R.id.card_harvest_text);
            card_activity_text = (TextView) v.findViewById(R.id.card_activity_text);
            card_inspection_text = (TextView) v.findViewById(R.id.card_inspection_text);
            card_food = v.findViewById(R.id.card_food);
            card_treatment = v.findViewById(R.id.card_treatment);
            card_harvest = v.findViewById(R.id.card_harvest);
            card_activity = v.findViewById(R.id.card_activity);
            card_inspection = v.findViewById(R.id.card_inspection);
            card_weather = v.findViewById(R.id.card_weather);
            card_weather_img = (ImageView) v.findViewById(R.id.card_weather_img);

            card_options.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.END);
            popup.inflate(R.menu.menu_card_more);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.menu_card_edit:
                            Intent intent = new Intent(context, NewEntryActivity.class);
                            intent.putExtra("edit", true);
                            intent.putExtra("LogEntry", mEntry.getId());
                            context.startActivity(intent);
                            return true;
                        case R.id.menu_card_delete:
                            LogActivity.mDB.deleteLog(mEntry.getId());
                            LogActivity.updateList();
                            return true;
                    }
                    return false;
                }
            });
            popup.show();

        }
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String[] dateArray = constraint.toString().split(";");
            Calendar cFrom = Calendar.getInstance();
            Calendar cTill = Calendar.getInstance();
            cFrom.setTimeInMillis(Long.parseLong(dateArray[0]));
            cTill.setTimeInMillis(Long.parseLong(dateArray[1]));

            cFrom.set(Calendar.HOUR, 0);
            cFrom.set(Calendar.MINUTE, 0);

            cTill.set(Calendar.HOUR, 0);
            cTill.set(Calendar.MINUTE, 0);
            cTill.set(Calendar.DAY_OF_MONTH, cTill.get(Calendar.DAY_OF_MONTH) + 1);

            FilterResults results = new FilterResults();

            final List<LogEntry> list = mList;

            int count = list.size();
            final List<LogEntry> nlist = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                Date filter = list.get(i).getDate();
                if (filter.after(cFrom.getTime()) && filter.before(cTill.getTime())) {
                    nlist.add(list.get(i));
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFiltered = (List<LogEntry>) results.values;
            notifyDataSetChanged();
        }

    }
}
