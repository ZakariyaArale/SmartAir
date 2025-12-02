package com.example.smartairsetup.provider_home_ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartairsetup.R;

import java.util.List;

public class ProviderPefLogAdapter extends RecyclerView.Adapter<ProviderPefLogAdapter.VH> {

    private final List<PefLogItem> items;

    public ProviderPefLogAdapter(List<PefLogItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pef_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PefLogItem item = items.get(position);

        h.date.setText(item.date != null ? item.date : "-");
        h.zone.setText("Zone: " + (item.zone != null ? item.zone : "-"));
        h.daily.setText("Daily PEF: " + item.dailyPEF);
        h.pb.setText("PB: " + item.pb);
        h.prepost.setText("Pre: " + item.prePEF + "   •   Post: " + item.postPEF);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, zone, daily, pb, prepost;

        VH(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.textDate);
            zone = itemView.findViewById(R.id.textZone);
            daily = itemView.findViewById(R.id.textDaily);
            pb = itemView.findViewById(R.id.textPb);
            prepost = itemView.findViewById(R.id.textPrePost);
        }
    }
}