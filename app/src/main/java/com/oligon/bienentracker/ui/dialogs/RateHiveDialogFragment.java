package com.oligon.bienentracker.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.object.Hive;


public class RateHiveDialogFragment extends DialogFragment implements RatingBar.OnRatingBarChangeListener {

    private Hive mHive;

    private OnDialogFinishedListener mListener;

    public RateHiveDialogFragment() {
    }

    public interface OnDialogFinishedListener {
        void onDialogFinished();
    }

    @Override
    public void onResume() {
        super.onResume();
        BeeApplication.getInstance().trackScreenView("Hive Rate Fragment");
    }

    @Override
    public void onAttach(Activity activity) {
        mListener = (OnDialogFinishedListener) activity;
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_rating, null);
        RatingBar mGentle = (RatingBar) view.findViewById(R.id.rating_gentle);
        RatingBar mEscape = (RatingBar) view.findViewById(R.id.rating_escape);
        RatingBar mStrength = (RatingBar) view.findViewById(R.id.rating_strength);
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RateHiveDialogFragment.this.getDialog().cancel();
                    }
                });
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            mHive = (Hive) args.getSerializable("hive");
        }
        if (mHive == null) return builder.create();
        mGentle.setRating(mHive.getRating(Hive.Rating.GENTLENESS));
        mEscape.setRating(mHive.getRating(Hive.Rating.ESCAPE));
        mStrength.setRating(mHive.getRating(Hive.Rating.STRENGTH));
        mGentle.setOnRatingBarChangeListener(this);
        mEscape.setOnRatingBarChangeListener(this);
        mStrength.setOnRatingBarChangeListener(this);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HiveDB.getInstance(getContext()).editHive(mHive);
                    mListener.onDialogFinished();
                    d.dismiss();
                }
            });
        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        switch (ratingBar.getId()) {
            case R.id.rating_gentle:
                mHive.setRating(Hive.Rating.GENTLENESS, rating);
                break;
            case R.id.rating_escape:
                mHive.setRating(Hive.Rating.ESCAPE, rating);
                break;
            case R.id.rating_strength:
                mHive.setRating(Hive.Rating.STRENGTH, rating);
                break;
        }
    }
}
