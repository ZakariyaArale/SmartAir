package com.example.smartairsetup.provider_home_ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartairsetup.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProviderControllerLogAdapter extends RecyclerView.Adapter<ProviderControllerLogAdapter.VH> {

    private final List<ControllerLogItem> items;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    public ProviderControllerLogAdapter(List<ControllerLogItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_controller_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ControllerLogItem item = items.get(position);

        h.medName.setText(item.medName);
        h.dose.setText("Dose: " + (item.doseText != null ? item.doseText : "-"));

        if (item.takenAt != null) {
            h.time.setText("Taken: " + fmt.format(item.takenAt));
        } else {
            h.time.setText("Taken: -");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView medName, time, dose;

        VH(@NonNull View itemView) {
            super(itemView);
            medName = itemView.findViewById(R.id.textMedName);
            time = itemView.findViewById(R.id.textTime);
            dose = itemView.findViewById(R.id.textDose);
        }
    }
}