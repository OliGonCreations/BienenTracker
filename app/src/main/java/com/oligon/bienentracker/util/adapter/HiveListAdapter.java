package com.oligon.bienentracker.util.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.object.LogEntry;
import com.oligon.bienentracker.util.Circle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HiveListAdapter extends RecyclerView.Adapter<HiveListAdapter.HiveViewHolder> {

    private Context context;
    private final List<Hive> mList;
    private final List<LogEntry> mLogs;
    private LogClickListener mListener;
    private SharedPreferences mPrefs;

    public HiveListAdapter(LogClickListener mListener) {
        this.mListener = mListener;
        mList = new ArrayList<>();
        mLogs = new ArrayList<>();
    }

    public void updateData(List<Hive> mList, List<LogEntry> mLogs) {
        this.mList.clear();
        this.mLogs.clear();
        this.mList.addAll(mList);
        this.mLogs.addAll(mLogs);
        notifyDataSetChanged();
    }

    @Override
    public HiveViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        View v = LayoutInflater.from(context)
                .inflate(R.layout.card_hive, parent, false);
        return new HiveViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final HiveViewHolder holder, int i) {
        LogEntry entry = mLogs.get(i);
        Hive hive = mList.get(i);

        holder.hive_name.setText(hive.getName());

        holder.hive_location.setText(hive.getLocation().length() > 0 ?
                hive.getLocation() : " - ");
        holder.hive_img.setImageResource(hive.isOffspring() ?
                R.drawable.ic_offspring : R.drawable.ic_beehive);
        if (hive.getYear() != 0) {
            holder.hive_circle.setYear(hive.getYear());
            holder.hive_circle.setLabel(hive.getMarker());
            holder.hive_year.setText(String.valueOf(hive.getYear()));
            holder.hive_circle.setVisibility(View.VISIBLE);
        } else {
            holder.hive_circle.setVisibility(View.INVISIBLE);
            holder.hive_year.setText("");
        }
        String[] ratings = context.getResources().getStringArray(R.array.prefs_rating_entries);
        int ratingView = Integer.parseInt(mPrefs.getString("pref_list_rating_view", "0"));
        holder.hive_rating_text.setText(ratings[ratingView]);
        holder.hive_rating.setRating(hive.getRating(Hive.Rating.values()[ratingView]));

        if (hive.hasReminder())
            holder.card_reminder.setImageResource(R.drawable.ic_reminder);
        else
            holder.card_reminder.setImageResource(R.drawable.ic_reminder_off);

        holder.item = hive;

        if (entry != null) {
            holder.card_hive_more.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());
            try {
                String date = context.getResources().getString(R.string.card_hive_date) + " " + sdf.format(entry.getDate());
                holder.card_hive_title.setText(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (entry.hasFood()) {
                holder.card_hive_food.setVisibility(View.VISIBLE);
                holder.card_hive_food_text.setText(entry.getFoodText());
            } else
                holder.card_hive_food.setVisibility(View.GONE);
            if (entry.hasHarvest()) {
                holder.card_hive_harvest.setVisibility(View.VISIBLE);
                holder.card_hive_harvest_text.setText(entry.getHarvestText(context));
            } else
                holder.card_hive_harvest.setVisibility(View.GONE);
            if (entry.hasTreatment()) {
                holder.card_hive_treatment.setVisibility(View.VISIBLE);
                holder.card_hive_treatment_text.setText(entry.getTreatmentText());
            } else
                holder.card_hive_treatment.setVisibility(View.GONE);
            if (entry.hasActivities()) {
                holder.card_hive_activity.setVisibility(View.VISIBLE);
                holder.card_hive_activity_text.setText(entry.getActivitiesText(context));
            } else
                holder.card_hive_activity.setVisibility(View.GONE);
            if (entry.hasInspection()) {
                holder.card_hive_inspection_text.setText(entry.getInspectionText());
                holder.card_hive_inspection.setVisibility(View.VISIBLE);
            } else
                holder.card_hive_inspection.setVisibility(View.GONE);
        } else {
            holder.card_hive_more.setVisibility(View.GONE);
            holder.card_hive_title.setText(R.string.log_empty_message);
        }

        holder.expandedView.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public interface LogClickListener {
        void onClick(View caller, final Hive hive);

        void onAddLogClick(final Hive hive); //, int pos);

        void onMoreLogClick(final Hive hive);

        void onAddReminderClick(final Hive hive);
    }

    public class HiveViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        //public CardView cardView;
        public View expandedView;
        public Hive item;
        public int position;
        private boolean rotationDirection = true;
        ObjectAnimator rotation;

        /* Collapsed Views */
        public TextView hive_name, hive_year, hive_location, hive_rating_text;
        public ImageView hive_img, card_expand;
        public Circle hive_circle;
        public ImageButton card_options, card_reminder;
        public Button card_hive_add, card_hive_more;

        public RatingBar hive_rating;

        /* Expanded Views */
        public TextView card_hive_title, card_hive_food_text, card_hive_treatment_text,
                card_hive_harvest_text, card_hive_activity_text, card_hive_inspection_text;
        public View card_hive_food, card_hive_treatment, card_hive_harvest, card_hive_activity, card_hive_inspection;

        HiveViewHolder(View v) {
            super(v);

            hive_name = (TextView) v.findViewById(R.id.card_hive_name);
            hive_year = (TextView) v.findViewById(R.id.card_hive_year);
            hive_location = (TextView) v.findViewById(R.id.card_hive_location);
            hive_img = (ImageView) v.findViewById(R.id.card_hive_img);
            hive_circle = (Circle) v.findViewById(R.id.card_hive_circle);
            hive_rating_text = (TextView) v.findViewById(R.id.card_hive_rating_text);
            hive_rating = (RatingBar) v.findViewById(R.id.card_hive_rating);

            card_options = (ImageButton) v.findViewById(R.id.card_options);
            card_reminder = (ImageButton) v.findViewById(R.id.card_reminder);
            card_expand = (ImageView) v.findViewById(R.id.card_expand);
            card_hive_add = (Button) v.findViewById(R.id.card_hive_add);
            card_hive_more = (Button) v.findViewById(R.id.card_hive_more);
            card_options.setOnClickListener(this);
            card_reminder.setOnClickListener(this);
            card_hive_add.setOnClickListener(this);
            card_hive_more.setOnClickListener(this);

            v.findViewById(R.id.card_hive_main).setOnClickListener(this);

            expandedView = v.findViewById(R.id.card_hive_expanded);

            card_hive_title = (TextView) v.findViewById(R.id.card_hive_title);
            card_hive_food_text = (TextView) v.findViewById(R.id.card_hive_food_text);
            card_hive_treatment_text = (TextView) v.findViewById(R.id.card_hive_treatment_text);
            card_hive_harvest_text = (TextView) v.findViewById(R.id.card_hive_harvest_text);
            card_hive_activity_text = (TextView) v.findViewById(R.id.card_hive_activity_text);
            card_hive_inspection_text = (TextView) v.findViewById(R.id.card_hive_inspection_text);
            card_hive_food = v.findViewById(R.id.card_hive_food);
            card_hive_treatment = v.findViewById(R.id.card_hive_treatment);
            card_hive_harvest = v.findViewById(R.id.card_hive_harvest);
            card_hive_activity = v.findViewById(R.id.card_hive_activity);
            card_hive_inspection = v.findViewById(R.id.card_hive_inspection);

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.card_options:
                    mListener.onClick(v, item);
                    break;
                case R.id.card_reminder:
                    mListener.onAddReminderClick(item);
                    break;
                case R.id.card_hive_add:
                    mListener.onAddLogClick(item);
                    break;
                case R.id.card_hive_more:
                    mListener.onMoreLogClick(item);
                    break;
                case R.id.card_hive_main:
                    toggleItem();
                    break;
                default:
                    break;
            }
        }

        public void toggleItem() {
            int from, to;
            if (rotationDirection) {
                from = 0;
                to = 180;
            } else {
                from = 180;
                to = 0;
            }
            rotationDirection = !rotationDirection;
            rotation = ObjectAnimator.ofFloat(card_expand, "rotation", from, to);
            rotation.setDuration(300);
            rotation.start();
            if (expandedView.getVisibility() == View.GONE) {
                expandedView.setVisibility(View.VISIBLE);
            } else {
                expandedView.setVisibility(View.GONE);
            }
        }
    }
}