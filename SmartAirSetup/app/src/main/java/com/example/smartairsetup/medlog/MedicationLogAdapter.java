package com.example.smartairsetup.medlog;

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

public class MedicationLogAdapter extends ArrayAdapter<MedicationLogEntry> {

    public MedicationLogAdapter(@NonNull Context context,
                                @NonNull List<MedicationLogEntry> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {

        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            rowView = inflater.inflate(R.layout.item_medication_log, parent, false);
        }

        MedicationLogEntry entry = getItem(position);
        if (entry == null) {
            return rowView;
        }

        TextView textChildName = rowView.findViewById(R.id.textChildName);
        TextView textDateTime = rowView.findViewById(R.id.textDateTime);
        TextView textMedType = rowView.findViewById(R.id.textMedType);
        TextView textDose = rowView.findViewById(R.id.textDose);

        // Top-left: medication name from medications collection
        textChildName.setText(entry.medName);

        // Top-right: date and time
        textDateTime.setText(entry.dateTime);

        // Second row left: medication type (Rescue / Controller)
        textMedType.setText(entry.medType);

        // Second row right: dose count
        textDose.setText(entry.doseCount + " puffs");

        return rowView;
    }
}
