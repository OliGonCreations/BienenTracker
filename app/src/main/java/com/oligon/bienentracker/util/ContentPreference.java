package com.oligon.bienentracker.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oligon.bienentracker.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ContentPreference extends DialogPreference implements View.OnClickListener, TextView.OnEditorActionListener, AdapterView.OnItemLongClickListener {

    private EditText mField;
    private SharedPreferences sp;
    private static Set<String> set = new TreeSet<>(), defaultSet;
    private List<String> arrayList = new ArrayList<>();
    private ArrayAdapter mAdapter;

    public ContentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        setDialogLayoutResource(R.layout.dialog_content_preference);

        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet(getKey(), null) == null) {
            if (getKey().equals("pref_list_food"))
                defaultSet = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.food)));
            else if (getKey().equals("pref_list_treatment"))
                defaultSet = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.drugs)));
            else
                defaultSet = new HashSet<>();
            sp.edit().putStringSet(getKey(), defaultSet).apply();
        }


    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Button mConfirm = (Button) view.findViewById(R.id.content_preference_button);
        mField = (EditText) view.findViewById(R.id.content_preference_field);
        ListView mList = (ListView) view.findViewById(R.id.content_preference_list);

        mConfirm.setOnClickListener(this);
        mField.setOnEditorActionListener(this);
        mList.setOnItemLongClickListener(this);

        set.clear();
        arrayList.clear();
        set.addAll(sp.getStringSet(getKey(), new TreeSet<String>()));
        arrayList.addAll(set);
        mAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, arrayList);
        mAdapter.notifyDataSetChanged();
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        addToList();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            addToList();
            return true;
        }
        return false;
    }

    private void addToList() {
        String field = mField.getText().toString();
        if (field.isEmpty()) {
            mField.setError(getContext().getString(R.string.error_et));
            return;
        }
        mField.setText("");
        set.add(field);
        arrayList.add(field);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            if (mField.getText().length() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogOrange);
                builder.setMessage(R.string.alert_discard_add_entry);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        set.add(mField.getText().toString());
                        sp.edit().putStringSet(getKey(), set).apply();
                        Toast.makeText(getContext(), R.string.prefs_entry_added, Toast.LENGTH_LONG).show();
                    }
                });
                builder.create().show();
            }
            sp.edit().putStringSet(getKey(), set).apply();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        PopupMenu popup = new PopupMenu(getContext(), view, Gravity.END);
        popup.inflate(R.menu.menu_delete);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                set.remove(arrayList.get(position));
                arrayList.remove(position);
                mAdapter.notifyDataSetChanged();
                return false;
            }
        });
        popup.show();
        return false;
    }
}
