package com.example.p2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private List<Map<String, Object>> historyList;

    public HistoryAdapter(List<Map<String, Object>> historyList) {
        this.historyList = historyList;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvBedtime, tvAfterBedtime;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBedtime = itemView.findViewById(R.id.tvBedtime);
            tvAfterBedtime = itemView.findViewById(R.id.tvAfterBedtime);
        }
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Map<String, Object> item = historyList.get(position);
        String date = (String) item.get("date");
        String bedtime = (String) item.get("bedtime");
        Long minutes = (Long) item.get("afterBedtimeMinutes");

        holder.tvDate.setText(date);
        holder.tvBedtime.setText("Bedtime: " + bedtime);
        holder.tvAfterBedtime.setText(minutes + "m");
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}