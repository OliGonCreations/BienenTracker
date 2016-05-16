package com.oligon.bienentracker.util.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.oligon.bienentracker.R;
import com.oligon.bienentracker.ui.activities.HomeActivity;
import com.oligon.bienentracker.ui.activities.SettingsActivity;
import com.oligon.bienentracker.util.HiveDB;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class RestoreListAdapter extends RecyclerView.Adapter<RestoreListAdapter.RestoreViewHolder> {

    private List<String> names;
    private List<File> files;
    public OnRestoreItemClickListener mListener;
    private Context context;

    public RestoreListAdapter(List<String> names, List<File> files) {
        this.names = names;
        this.files = files;
    }

    public interface OnRestoreItemClickListener {
        void onClick(int pos);

        void onLongClick(View v, int pos);
    }

    @Override
    public RestoreViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        mListener = new OnRestoreItemClickListener() {
            @Override
            public void onClick(final int pos) {
                restoreSQL(pos);
            }

            @Override
            public void onLongClick(View v, final int pos) {
                PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.END);
                popup.inflate(R.menu.menu_restore);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_action_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                                builder.setMessage(R.string.alert_delete_db);
                                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (files.get(0).exists())
                                            if (files.get(pos).delete())
                                                SettingsActivity.BackupDialogFragment.updateUI();
                                        dialog.dismiss();
                                    }
                                });
                                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                builder.create().show();
                                return true;
                            case R.id.menu_action_send:
                                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                                emailIntent.setType("text/plain");

                                Uri uri = Uri.parse("file://" + files.get(pos).getAbsolutePath());
                                emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                context.startActivity(emailIntent);
                                return true;
                            case R.id.menu_action_restore:
                                restoreSQL(pos);
                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        };
        View v = LayoutInflater.from(context)
                .inflate(R.layout.list_restore, parent, false);
        return new RestoreViewHolder(v, mListener);
    }

    private void restoreSQL(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
        builder.setMessage(R.string.alert_restore_db);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    SettingsActivity.restoreSQL(files.get(pos));
                    HomeActivity.dbChanged = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    HiveDB.forceUpgrade();
                    Toast.makeText(context, R.string.successfully_restored, Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onBindViewHolder(RestoreViewHolder holder, int position) {
        holder.mTitle.setText(names.get(position));
    }

    @Override
    public int getItemCount() {
        return names.size();
    }


    public class RestoreViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        public TextView mTitle;
        private OnRestoreItemClickListener mListener;

        public RestoreViewHolder(View v, OnRestoreItemClickListener mListener) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            this.mListener = mListener;
            mTitle = (TextView) v.findViewById(R.id.list_restore_text);
        }

        @Override
        public void onClick(View v) {
            mListener.onClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            mListener.onLongClick(v, getAdapterPosition());
            return true;
        }
    }
}
