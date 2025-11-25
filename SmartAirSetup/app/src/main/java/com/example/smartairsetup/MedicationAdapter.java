package com.example.smartairsetup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedViewHolder> {

    private List<Medication> meds;
    private OnMedClickListener listener;

    public MedicationAdapter(List<Medication> meds, OnMedClickListener listener) {

        this.meds = meds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the row layout. Make sure the resource name matches your file in res/layout.
        // If your row file is med_item.xml, use R.layout.med_item.
        // If it's item_medication.xml, change R.layout.med_item here to R.layout.item_medication.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.med_item_in_inventory_list, parent, false);
        return new MedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        Medication med = meds.get(position);

        // Use whatever accessor your Medication class provides:
        // - If Medication has public field `name`: use med.name
        // - If Medication has getter getName(): use med.getName()
        // I will assume public field `name` (as in the earlier example).
        holder.medNameText.setText(med.getName());

        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onMedClick(med);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meds != null ? meds.size() : 0;
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {

        TextView medNameText;

        public MedViewHolder(View itemView) {
            super(itemView);
            medNameText = itemView.findViewById(R.id.medNameText);
        }
    }

    public interface OnMedClickListener {
        void onMedClick(Medication med);
    }
}
