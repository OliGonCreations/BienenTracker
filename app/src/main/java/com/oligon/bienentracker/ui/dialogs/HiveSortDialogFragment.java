package com.oligon.bienentracker.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.ui.activities.HomeActivity;
import com.oligon.bienentracker.util.OnDialogFinishedListener;
import com.oligon.bienentracker.util.adapter.ItemTouchHelperAdapter;
import com.oligon.bienentracker.util.adapter.ItemTouchHelperViewHolder;
import com.oligon.bienentracker.util.adapter.OnStartDragListener;
import com.oligon.bienentracker.util.adapter.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HiveSortDialogFragment extends DialogFragment implements OnStartDragListener {

    private OnDialogFinishedListener mListener;
    private ItemTouchHelper mItemTouchHelper;
    private static RecyclerListAdapter adapter;

    public HiveSortDialogFragment() {
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
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
        BeeApplication.getInstance().trackScreenView("Hive Sort Dialog Fragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        List<Hive> mHives = HomeActivity.db.getAllHives();
        adapter = new RecyclerListAdapter(mHives, this);

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_sort, null);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.dialog_sort_list);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        if (mHives.size() == 0) {
            view.findViewById(R.id.dialog_sort_empty).setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        HiveSortDialogFragment.this.getDialog().cancel();
                    }
                });
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
                    for (int i = 0; i < adapter.mIds.size(); i++) {
                        HomeActivity.db.updateHivePosition(adapter.mIds.get(i), i);
                    }
                    mListener.onDialogFinished();
                    d.dismiss();
                }
            });
        }
    }

    private class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>
            implements ItemTouchHelperAdapter {

        private final List<String> mItems = new ArrayList<>();
        public final List<Integer> mIds = new ArrayList<>();

        private final OnStartDragListener mDragStartListener;

        public RecyclerListAdapter(List<Hive> hives, OnStartDragListener dragStartListener) {
            mDragStartListener = dragStartListener;
            for (Hive hive : hives) {
                mItems.add(hive.getName());
                mIds.add(hive.getId());
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_sort_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, int position) {
            holder.item.setText(mItems.get(position));
            holder.handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mItems, fromPosition, toPosition);
            Collections.swap(mIds, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder implements
                ItemTouchHelperViewHolder {
            public TextView item;
            public View handle;

            public ItemViewHolder(View itemView) {
                super(itemView);
                handle = itemView.findViewById(R.id.list_sort_item);
                item = (TextView) itemView.findViewById(R.id.list_sort_item_text);
            }

            @Override
            public void onItemSelected() {
                itemView.setBackgroundColor(Color.LTGRAY);
            }

            @Override
            public void onItemClear() {
                itemView.setBackgroundColor(0);
            }
        }
    }
}
