package com.oligon.bienentracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.oligon.bienentracker.util.object.Hive;

public class HiveDialogFragment extends DialogFragment {

    private TextInputLayout labelName, labelYear;
    private EditText etName, etPosition, etYear, etMarker, etInfo;
    private CheckBox cbOffspring;
    private int hiveId = -1;

    private OnDialogFinishedListener mListener;

    public HiveDialogFragment() {
    }

    public interface OnDialogFinishedListener {
        void onDialogFinished();
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
    @Override
    public void onResume() {
        super.onResume();
        BeeApplication.getInstance().trackScreenView("Hive Dialog Fragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_hive, null);

        etName = (EditText) view.findViewById(R.id.et_hive_name);
        etPosition = (EditText) view.findViewById(R.id.et_hive_position);
        etYear = (EditText) view.findViewById(R.id.et_hive_year);
        etMarker = (EditText) view.findViewById(R.id.et_hive_marker);
        etInfo = (EditText) view.findViewById(R.id.et_hive_info);
        cbOffspring = (CheckBox) view.findViewById(R.id.hive_offspring);
        labelName = (TextInputLayout) view.findViewById(R.id.label_hive_name);
        labelYear = (TextInputLayout) view.findViewById(R.id.label_hive_year);

        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        HiveDialogFragment.this.getDialog().cancel();
                    }
                });
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            etName.setText(args.getString("name"), TextView.BufferType.EDITABLE);
            etPosition.setText(args.getString("position"), TextView.BufferType.EDITABLE);
            etYear.setText(String.valueOf(args.getInt("year") != 0 ? args.getInt("year") : ""), TextView.BufferType.EDITABLE);
            etInfo.setText(args.getString("info"), TextView.BufferType.EDITABLE);
            etMarker.setText(args.getString("marker"), TextView.BufferType.EDITABLE);
            cbOffspring.setChecked(args.getBoolean("offspring"));
            hiveId = args.getInt("id");
        }

        etName.addTextChangedListener(new NameTextWatcher());
        etYear.addTextChangedListener(new YearTextWatcher());

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
                    Hive hive = new Hive(hiveId);
                    if (!validateName())
                        return;
                    if (!validateYear())
                        return;

                    hive.setName(etName.getText().toString());
                    hive.setLocation(etPosition.getText().toString());

                    if (!etYear.getText().toString().isEmpty()) {
                        hive.setYear(Integer.parseInt(etYear.getText().toString()));
                    }
                    hive.setMarker(etMarker.getText().toString());
                    hive.setInfo(etInfo.getText().toString());
                    hive.setType(cbOffspring.isChecked());

                    if (hiveId != -1)
                        HomeActivity.db.editHive(hive);
                    else {
                        HomeActivity.db.addHive(hive);
                    }
                    mListener.onDialogFinished();

                    d.dismiss();
                }
            });
        }
    }

    private boolean validateName() {
        if (etName.getText().toString().trim().isEmpty()) {
            labelName.setError(getString(R.string.error_msg_name));
            labelName.setErrorEnabled(true);
            requestFocus(etName);
            return false;
        } else {
            labelName.setError(null);
            labelName.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateYear() {
        if (!etYear.getText().toString().isEmpty()) {
            if (etYear.getText().toString().length() != 4) {
                labelYear.setError(getString(R.string.error_msg_year));
                labelYear.setErrorEnabled(true);
                requestFocus(etYear);
                return false;
            } else {
                labelYear.setError(null);
                labelYear.setErrorEnabled(false);
            }

        }
        return true;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    private class NameTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            validateName();
        }
    }

    private class YearTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            validateYear();
        }
    }
}