package com.example.smartairsetup.medlog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartairsetup.R;

import java.util.List;


public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedViewHolder> {

    private List<ChildMedicationWrapper> meds;
    private OnMedClickListener listener;

    private int selectedPosition = RecyclerView.NO_POSITION;

    public MedicationAdapter(List<ChildMedicationWrapper> meds, OnMedClickListener listener) {

        this.meds = meds;
        this.listener = listener;
    }

    @Override
    public MedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicaton_listlinearlayout, parent, false);
        return new MedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        ChildMedicationWrapper childMeds = meds.get(position);

        holder.medNameTV.setText(childMeds.getMed().getName());
        holder.childNameTV.setText(childMeds.getChildName());

        // Apply selected state to trigger selector drawable
        holder.itemView.setSelected(position == selectedPosition);

        holder.itemView.setOnClickListener(view -> {

            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) return;

            int oldPosition = selectedPosition;
            selectedPosition = clickedPosition;

            if (listener != null) {
                listener.onMedClick(childMeds);
            }

            // Refresh only what changed
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

        });

    }

    @Override
    public int getItemCount() {
        return meds != null ? meds.size() : 0;
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {

        TextView medNameTV;
        TextView childNameTV;

        public MedViewHolder(View itemView) {
            super(itemView);
            medNameTV = itemView.findViewById(R.id.medNameTV);
            childNameTV = itemView.findViewById(R.id.childNameTV);
        }
    }

    public interface OnMedClickListener {
        void onMedClick(ChildMedicationWrapper med);
    }
}
