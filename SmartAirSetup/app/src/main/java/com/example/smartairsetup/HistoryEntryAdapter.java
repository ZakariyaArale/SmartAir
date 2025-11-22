package com.example.smartairsetup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        textNight.setText("Night: " + entry.night);
        textActivity.setText("Activity: " + entry.activity);
        textCough.setText("Cough/wheeze: " + entry.cough);
        textTriggers.setText("Triggers: " + entry.triggers);
        textAuthor.setText("Author: " + entry.author);

        return view;
    }
}
