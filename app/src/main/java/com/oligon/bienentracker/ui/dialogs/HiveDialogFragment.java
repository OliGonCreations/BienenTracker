package com.oligon.bienentracker.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.ui.activities.HomeActivity;
import com.oligon.bienentracker.util.OnDialogFinishedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HiveDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private TextInputLayout labelName, labelYear;
    private EditText etName, etPosition, etYear, etMarker, etInfo;
    private CheckBox cbOffspring;
    private Spinner spGroup;
    private Hive mHive = new Hive(-1);

    private OnDialogFinishedListener mListener;

    public HiveDialogFragment() {
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
        spGroup = (Spinner) view.findViewById(R.id.hive_group);
        labelName = (TextInputLayout) view.findViewById(R.id.label_hive_name);
        labelYear = (TextInputLayout) view.findViewById(R.id.label_hive_year);

        spGroup.setOnItemSelectedListener(this);


        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        HiveDialogFragment.this.getDialog().cancel();
                    }
                });
        Bundle args = getArguments();
        if (args != null && !args.isEmpty()) {
            mHive = (Hive) args.getSerializable("hive");
        }

        updateUI();

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
                    if (!validateName())
                        return;
                    if (!validateYear())
                        return;

                    mHive.setName(etName.getText().toString());
                    mHive.setLocation(etPosition.getText().toString());

                    if (!etYear.getText().toString().isEmpty()) {
                        mHive.setYear(Integer.parseInt(etYear.getText().toString()));
                    }
                    mHive.setMarker(etMarker.getText().toString());
                    mHive.setInfo(etInfo.getText().toString());
                    mHive.setType(cbOffspring.isChecked());
                    if (spGroup.getSelectedItemPosition() != 0)
                        mHive.setGroup(String.valueOf(spGroup.getSelectedItem()));
                    else mHive.setGroup("");

                    HomeActivity.db.editHive(mHive);
                    mListener.onDialogFinished();
                    d.dismiss();
                }
            });
        }
    }

    private void updateUI() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> set = sp.getStringSet("pref_list_groups", new HashSet<String>());

        List<String> content = new ArrayList<>(set);
        Collections.sort(content);
        content.add(0, getString(R.string.no_group));
        ArrayAdapter adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, content);
        spGroup.setAdapter(adapter);

        etName.setText(mHive.getName(), TextView.BufferType.EDITABLE);
        etPosition.setText(mHive.getLocation(), TextView.BufferType.EDITABLE);
        etYear.setText(String.valueOf(mHive.getYear() != 0 ? mHive.getYear() : ""), TextView.BufferType.EDITABLE);
        etInfo.setText(mHive.getInfo(), TextView.BufferType.EDITABLE);
        etMarker.setText(mHive.getMarker(), TextView.BufferType.EDITABLE);
        cbOffspring.setChecked(mHive.isOffspring());
        spGroup.setSelection(content.indexOf(mHive.getGroup()));
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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