package com.oligon.bienentracker.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.oligon.bienentracker.ui.activities.NewEntryActivity;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.util.InstantAutoCompleteTextView;
import com.oligon.bienentracker.object.Activities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CommonDialogFragment extends DialogFragment {

    Activities act = new Activities();

    private final static int ACTIVITIES = 10;

    private InstantAutoCompleteTextView etOther;
    private final TextView[] tv = new TextView[ACTIVITIES];
    private final Button[] btnP = new Button[ACTIVITIES];
    private final Button[] btnM = new Button[ACTIVITIES];
    private final static int[] btnPId = new int[]{
            R.id.btnPDrone,
            R.id.btnPBrood,
            R.id.btnPEmpty,
            R.id.btnPFood,
            R.id.btnPMiddle,
            R.id.btnPHoney,
            R.id.btnPStock,
            R.id.btnPEscape,
            R.id.btnPFence,
            R.id.btnPDiaper
    };
    private final int[] btnMId = new int[]{
            R.id.btnMDrone,
            R.id.btnMBrood,
            R.id.btnMEmpty,
            R.id.btnMFood,
            R.id.btnMMiddle,
            R.id.btnMHoney,
            R.id.btnMStock,
            R.id.btnMEscape,
            R.id.btnMFence,
            R.id.btnMDiaper
    };
    private final int[] tvId = new int[]{
            R.id.tvDrone,
            R.id.tvBrood,
            R.id.tvEmpty,
            R.id.tvFood,
            R.id.tvMiddle,
            R.id.tvHoney,
            R.id.tvStock,
            R.id.tvEscape,
            R.id.tvFence,
            R.id.tvDiaper
    };

    private View.OnClickListener mPClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnPDrone:
                    act.setDrones(act.getDrones() + 1);
                    break;
                case R.id.btnPBrood:
                    act.setBrood(act.getBrood() + 1);
                    break;
                case R.id.btnPEmpty:
                    act.setEmpty(act.getEmpty() + 1);
                    break;
                case R.id.btnPFood:
                    act.setFood(act.getFood() + 1);
                    break;
                case R.id.btnPMiddle:
                    act.setMiddle(act.getMiddle() + 1);
                    break;
                case R.id.btnPHoney:
                    act.setHoneyRoom(act.getHoneyRoom() + 1);
                    break;
                case R.id.btnPStock:
                    act.setBox(act.getBox() + 1);
                    break;
                case R.id.btnPEscape:
                    act.setEscape(act.getEscape() + 1);
                    break;
                case R.id.btnPFence:
                    act.setFence(act.getFence() + 1);
                    break;
                case R.id.btnPDiaper:
                    act.setDiaper(act.getDiaper() + 1);
                default:
                    break;
            }
            updateUI();
        }
    };

    private View.OnClickListener mMClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnMDrone:
                    act.setDrones(act.getDrones() - 1);
                    break;
                case R.id.btnMBrood:
                    act.setBrood(act.getBrood() - 1);
                    break;
                case R.id.btnMEmpty:
                    act.setEmpty(act.getEmpty() - 1);
                    break;
                case R.id.btnMFood:
                    act.setFood(act.getFood() - 1);
                    break;
                case R.id.btnMMiddle:
                    act.setMiddle(act.getMiddle() - 1);
                    break;
                case R.id.btnMHoney:
                    act.setHoneyRoom(act.getHoneyRoom() - 1);
                    break;
                case R.id.btnMStock:
                    act.setBox(act.getBox() - 1);
                    break;
                case R.id.btnMEscape:
                    act.setEscape(act.getEscape() - 1);
                    break;
                case R.id.btnMFence:
                    act.setFence(act.getFence() - 1);
                    break;
                case R.id.btnMDiaper:
                    act.setDiaper(act.getDiaper() - 1);
                    break;
                default:
                    break;
            }
            updateUI();
        }
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogGreen);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_common, null);

        for (int i = 0; i < tv.length; i++) {
            tv[i] = (TextView) view.findViewById(tvId[i]);
            btnP[i] = (Button) view.findViewById(btnPId[i]);
            btnM[i] = (Button) view.findViewById(btnMId[i]);
            btnP[i].setOnClickListener(mPClickListener);
            btnM[i].setOnClickListener(mMClickListener);
        }

        etOther = (InstantAutoCompleteTextView) view.findViewById(R.id.et_common_other);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        List<String> list = new ArrayList<>(sp.getStringSet("pref_list_activities", new HashSet<String>()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, list);

        etOther.setAdapter(adapter);
        etOther.setThreshold(1);

        Bundle args = getArguments();
        if (args != null) {
            act = (Activities) args.getSerializable("activities");
            updateUI();
            etOther.setText(act.getOther());
        }

        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        act.setOther(etOther.getText().toString());
                        NewEntryActivity.mLogEntry.setCommonActivities(act);
                        NewEntryActivity.dataSetChanged();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CommonDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    private void updateUI() {
        tv[0].setText(String.valueOf(act.getDrones()));
        tv[1].setText(String.valueOf(act.getBrood()));
        tv[2].setText(String.valueOf(act.getEmpty()));
        tv[3].setText(String.valueOf(act.getFood()));
        tv[4].setText(String.valueOf(act.getMiddle()));
        tv[5].setText(String.valueOf(act.getHoneyRoom()));
        tv[6].setText(String.valueOf(act.getBox()));
        tv[7].setText(String.valueOf(act.getEscape()));
        tv[8].setText(String.valueOf(act.getFence()));
        tv[9].setText(String.valueOf(act.getDiaper()));

        if (act.getEscape() == 1) btnP[7].setEnabled(false);
        else btnP[7].setEnabled(true);
        if (act.getEscape() == -1) btnM[7].setEnabled(false);
        else btnM[7].setEnabled(true);

        if (act.getFence() == 1) btnP[8].setEnabled(false);
        else btnP[8].setEnabled(true);
        if (act.getFence() == -1) btnM[8].setEnabled(false);
        else btnM[8].setEnabled(true);

        if (act.getDiaper() == 1) btnP[9].setEnabled(false);
        else btnP[9].setEnabled(true);
        if (act.getDiaper() == -1) btnM[9].setEnabled(false);
        else btnM[9].setEnabled(true);
    }
}
