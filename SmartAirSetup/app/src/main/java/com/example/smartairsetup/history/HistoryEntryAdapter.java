package com.example.smartairsetup.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.smartairsetup.R;

import java.util.List;

public class HistoryEntryAdapter extends ArrayAdapter<HistoryEntry> {

    public HistoryEntryAdapter(@NonNull Context context, @NonNull List<HistoryEntry> entries) {
        super(context, 0, entries);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_history_entry, parent, false);
        }

        HistoryEntry entry = getItem(position);
        if (entry == null) return view;

        TextView textChild = view.findViewById(R.id.textCardChild);
        TextView textDate = view.findViewById(R.id.textCardDate);
        TextView textNight = view.findViewById(R.id.textCardNight);
        TextView textActivity = view.findViewById(R.id.textCardActivity);
        TextView textCough = view.findViewById(R.id.textCardCough);
        TextView textTriggers = view.findViewById(R.id.textCardTriggers);
        TextView textAuthor = view.findViewById(R.id.textCardAuthor);

        textChild.setText(entry.childName);
        textDate.setText(entry.date);
        textNight.setText(getContext().getString(R.string.history_night, entry.night));
        textActivity.setText(getContext().getString(R.string.history_activity, entry.activity));
        textCough.setText(getContext().getString(R.string.history_cough, entry.cough));
        textTriggers.setText(getContext().getString(R.string.history_triggers, entry.triggers));
        textAuthor.setText(getContext().getString(R.string.history_author, entry.author));

        return view;
    }
}
