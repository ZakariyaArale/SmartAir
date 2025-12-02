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

public class ProviderTriageIncidentAdapter extends RecyclerView.Adapter<ProviderTriageIncidentAdapter.VH> {

    private final List<TriageIncidentItem> items;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    public ProviderTriageIncidentAdapter(List<TriageIncidentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_triage_incident, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TriageIncidentItem item = items.get(position);

        String dateLine = (item.dateStr != null && !item.dateStr.isEmpty())
                ? item.dateStr
                : "Unknown date";

        h.textDate.setText("Date: " + dateLine);
        h.textZone.setText("Zone: " + (item.zone != null ? item.zone : "-"));

        String pefLine = "Daily PEF: " + (item.dailyPEF != null ? item.dailyPEF : "-")
                + "  •  PB: " + (item.pb != null ? item.pb : "-")
                + "\nPre: " + (item.prePEF != null ? item.prePEF : "-")
                + "  •  Post: " + (item.postPEF != null ? item.postPEF : "-");
        h.textDetails.setText(pefLine);

        if (item.takenAt != null) {
            h.textTime.setText("Recorded: " + fmt.format(item.takenAt));
        } else {
            h.textTime.setText("Recorded: -");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView textDate, textZone, textTime, textDetails;

        VH(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textZone = itemView.findViewById(R.id.textZone);
            textTime = itemView.findViewById(R.id.textTime);
            textDetails = itemView.findViewById(R.id.textDetails);
        }
    }
}