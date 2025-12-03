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

public class ProviderRescueLogAdapter extends RecyclerView.Adapter<ProviderRescueLogAdapter.VH> {

    private final List<RescueLogItem> items;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    public ProviderRescueLogAdapter(List<RescueLogItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rescue_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RescueLogItem item = items.get(position);

        String name = (item.medName != null && !item.medName.trim().isEmpty())
                ? item.medName
                : "Rescue medication";
        h.medName.setText(name);

        h.dose.setText("Dose: " + (item.doseCount > 0 ? item.doseCount : "-"));

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